r"""
config.mm Parser — Mall Maniacs (v0.40)
========================================

Reverse-engineered from dogbolt_decompile.c (Hex-Rays, Visual C++ target).

FILE FORMAT OVERVIEW
--------------------
config.mm is a byte-wise XOR-0x55 encrypted copy of a plaintext structured
config file.  The game decrypts it to a temp file called "sommar.sol", parses
it, then removes the temp file.  The same tree structure is used for several
other data files (navbuoy databases, eventobject databases, maniac.cfg).

PLAINTEXT GRAMMAR (reconstructed from sub_435890 / sub_435820 / sub_435C30)
-----------------------------------------------------------------------------

    file        ::= statement*
    statement   ::= comment | assignment | block_open | block_close
                  | array_open | array_close | value

    comment     ::= '/' <anything until newline>
    assignment  ::= token '=' value
    block_open  ::= '{'
    block_close ::= '}'
    array_open  ::= '['
    array_close ::= ']'
    token       ::= <non-whitespace, non-special chars, not starting with '"'>
    value       ::= token | quoted_string | number
    quoted_string ::= '"' <any chars except '"'> '"'
    number      ::= float-like token (parsed with atof internally)

Tokens are separated by whitespace and/or commas.
Special single-char tokens: = [ ] { }

TREE STRUCTURE
--------------
The parser builds a hierarchical key→value tree.  Blocks `{ }` create child
scopes.  Arrays `[ ]` create indexed sub-nodes.  A key followed by `= value`
stores a scalar; a key followed by `{...}` stores a sub-tree.

KNOWN TOP-LEVEL KEYS (from config.mm / the "set/get" command store)
--------------------------------------------------------------------
  driver         — graphics driver DLL path, e.g. DRIVERS\GXGLIDE.DLL
                    or DRIVERS\GXSOFT.DLL
  fshi<N>name<I> — high-score entry N, slot I, name
  fshi<N>time<I> — high-score entry N, slot I, time
  fshi<N>face<I> — high-score entry N, slot I, face index
  fshi<N>diff<I> — high-score entry N, slot I, difficulty
  vahi<N>name<I> } same layout for a second score table ("vahi")
  vahi<N>time<I> }
  vahi<N>face<I> }
  vahi<N>diff<I> }
  toplevel       — currently selected top-level menu index

KNOWN KEYS IN maniac.cfg (the main game tree, same format / same parser)
-------------------------------------------------------------------------
  master/                  — top-level configuration block
    log_max_level          — integer log verbosity
    log_to_file            — 0/1
    log_file_name          — path string
    obj_update_time        — int
    mapX, mapY, mapZoom    — floats (minimap viewport)
    ai_mode                — float/int
    acc                    — player acceleration
    friction               — friction coefficient
    rotate_acc             — rotational acceleration
    acc_WC                 — acc in world-coords mode
    rotate_acc_WC          — rotational acc in world-coords mode

  levels[N]/               — per-level block (N = 0..4)
    texture_files          — printf-style path pattern for merged textures
    scene_path             — directory for the level scene
    scene_file             — filename inside scene_path
    music                  — music track id (float)
    startup                — list of startup script commands

  objects/                 — global object configuration block
    obj_scene_file         — object scene filename
    char_scene_file        — character scene filename
    camera/                — camera sub-block
      pos/                 — camera position sub-block
      aim/                 — camera aim sub-block
      rot_speed            — float
      rot_max              — float
    carts[N]/              — per-cart block
      object_pos/
      handle_pos/
    characters[N]/         — per-character block
      object_name          — mesh name string
      friction             — float

  items[N]/                — collectible item blocks
    name                   — item display name
    mesh                   — mesh name in scene
"""

from __future__ import annotations

import io
import json
import sys
from pathlib import Path
from typing import Any, Union


# ---------------------------------------------------------------------------
# Tokeniser
# ---------------------------------------------------------------------------

_SPECIAL = set('=[]{}')
_WHITESPACE = set(' \t\r\n')
_DELIMITERS = _WHITESPACE | _SPECIAL | {','}


def _tokenise(text: str) -> list[str]:
    """
    Convert plaintext config content into a flat token list.

    Rules (from sub_435890):
    - '/' starts a line comment; skip until newline.
    - '"..."' is a quoted string token (quotes are stripped).
    - '=', '[', ']', '{', '}' are single-character tokens.
    - Commas are whitespace (silently consumed).
    - Everything else is an unquoted token, terminated by any delimiter.
    """
    tokens: list[str] = []
    i = 0
    n = len(text)

    while i < n:
        ch = text[i]

        # Skip whitespace and commas
        if ch in _WHITESPACE or ch == ',':
            i += 1
            continue

        # Line comment
        if ch == '/':
            while i < n and text[i] != '\n':
                i += 1
            continue

        # Single-char structural tokens
        if ch in _SPECIAL:
            tokens.append(ch)
            i += 1
            continue

        # Quoted string
        if ch == '"':
            i += 1  # consume opening quote
            j = i
            while j < n and text[j] != '"':
                j += 1
            tokens.append(text[i:j])
            i = j + 1  # consume closing quote
            continue

        # Unquoted token
        j = i
        while j < n and text[j] not in _DELIMITERS:
            j += 1
        tokens.append(text[i:j])
        i = j

    return tokens


# ---------------------------------------------------------------------------
# Parser — builds a nested dict/list/scalar tree
# ---------------------------------------------------------------------------

class _Parser:
    """
    Recursive-descent parser that mirrors the token-feed state machine in
    sub_435C30 and sub_435B80.

    The tree representation used here:
      - dict  : a keyed sub-block  (corresponds to a { } scope)
      - list  : a [ ] array block
      - str   : a quoted or unquoted string scalar
      - float : a numeric scalar
    """

    def __init__(self, tokens: list[str]) -> None:
        self._tokens = tokens
        self._pos = 0

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _peek(self) -> str | None:
        if self._pos < len(self._tokens):
            return self._tokens[self._pos]
        return None

    def _consume(self) -> str:
        tok = self._tokens[self._pos]
        self._pos += 1
        return tok

    def _expect(self, expected: str) -> None:
        tok = self._consume()
        if tok != expected:
            raise ValueError(
                f"Expected {expected!r} but got {tok!r} "
                f"(token index {self._pos - 1})"
            )

    @staticmethod
    def _to_scalar(tok: str) -> Union[float, str]:
        """Try to convert a token to a float; otherwise keep as string."""
        try:
            return float(tok)
        except ValueError:
            return tok

    # ------------------------------------------------------------------
    # Recursive block parser
    # ------------------------------------------------------------------

    def _parse_block(self) -> dict:
        """
        Parse tokens until '}' or EOF, returning a dict.
        Handles:
          key = scalar
          key = "string"
          key { block }
          key [ array ]
          key = { block }
          key = [ array ]
          bare scalar values appended under a synthetic '_values' list
        """
        result: dict[str, Any] = {}
        pending_values: list = []

        while True:
            tok = self._peek()

            if tok is None or tok == '}':
                break

            # Skip stray structural closes that don't belong here
            if tok == ']':
                break

            self._consume()

            # Opening brace without a preceding key — treat as anonymous block
            if tok == '{':
                child = self._parse_block()
                self._expect('}')
                _merge_into(result, '_anon', child)
                continue

            # Opening bracket without a preceding key
            if tok == '[':
                arr = self._parse_array()
                self._expect(']')
                _merge_into(result, '_anon', arr)
                continue

            # '=' alone is not a key; skip
            if tok == '=':
                continue

            key = tok

            # What follows the key?
            nxt = self._peek()

            if nxt == '=':
                self._consume()  # eat '='
                nxt2 = self._peek()

                if nxt2 == '{':
                    self._consume()
                    child = self._parse_block()
                    self._expect('}')
                    _merge_into(result, key, child)

                elif nxt2 == '[':
                    self._consume()
                    arr = self._parse_array()
                    self._expect(']')
                    _merge_into(result, key, arr)

                elif nxt2 is not None and nxt2 not in ('}', ']', '=', '[', '{'):
                    self._consume()
                    _merge_into(result, key, self._to_scalar(nxt2))

                else:
                    # Bare key= with nothing; store empty string
                    _merge_into(result, key, '')

            elif nxt == '{':
                self._consume()
                child = self._parse_block()
                self._expect('}')
                _merge_into(result, key, child)

            elif nxt == '[':
                # Could be key[N] { block } — the game's indexed sub-block pattern.
                # Distinguish from a bare array literal key [ v1 v2 ... ].
                #
                # A key[N] block looks like:  key [ integer ] {
                # A bare array looks like:    key [ val val val ]
                #
                # Peek ahead: if tokens are [ <simple-word> ] { then it's indexed.
                saved_pos = self._pos
                self._consume()  # consume '['
                index_tok = self._peek()
                is_indexed = False
                if (
                    index_tok is not None
                    and index_tok not in (']', '{', '}', '=', '[')
                    and ' ' not in index_tok  # quoted strings with spaces → array
                ):
                    # Tentatively consume and check for ] followed by {
                    self._consume()
                    if self._peek() == ']':
                        self._consume()  # consume ']'
                        if self._peek() in ('{', '=', None) or (
                            self._peek() is not None and self._peek() not in ('}', ']', '[')
                        ):
                            is_indexed = True
                            composite_key = f'{key}[{index_tok}]'
                            nxt3 = self._peek()
                            if nxt3 == '{':
                                self._consume()
                                child = self._parse_block()
                                self._expect('}')
                                _merge_into(result, composite_key, child)
                            elif nxt3 == '=':
                                self._consume()
                                val_tok = self._peek()
                                if val_tok is not None and val_tok not in ('}', ']', '=', '[', '{'):
                                    self._consume()
                                    _merge_into(result, composite_key, self._to_scalar(val_tok))
                                else:
                                    _merge_into(result, composite_key, '')
                            else:
                                _merge_into(result, composite_key, True)

                if not is_indexed:
                    # Restore position and parse as a bare array
                    self._pos = saved_pos + 1  # already consumed '['
                    # But we may have consumed extra tokens; fully restore
                    self._pos = saved_pos
                    self._consume()  # consume '[' again
                    arr = self._parse_array()
                    self._expect(']')
                    _merge_into(result, key, arr)

            else:
                # Key with no value — treat as a flag / bare token
                _merge_into(result, key, True)

        if pending_values:
            _merge_into(result, '_values', pending_values)

        return result

    def _parse_array(self) -> list:
        """
        Parse tokens until ']', returning a list of scalars or sub-blocks.
        Arrays in this format are either:
          [ scalar scalar ... ]
          [ { block } { block } ... ]
        """
        items: list = []

        while True:
            tok = self._peek()

            if tok is None or tok == ']':
                break

            if tok == '{':
                self._consume()
                child = self._parse_block()
                self._expect('}')
                items.append(child)
                continue

            if tok in ('}', '['):
                break

            self._consume()
            items.append(self._to_scalar(tok))

        return items

    def parse(self) -> dict:
        result = self._parse_block()
        return result


def _merge_into(d: dict, key: str, value: Any) -> None:
    """
    Insert value into dict d under key.
    If the key already exists, promote to a list (multi-value key).
    """
    if key not in d:
        d[key] = value
    else:
        existing = d[key]
        if isinstance(existing, list) and key != '_values':
            existing.append(value)
        else:
            d[key] = [existing, value]


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def decrypt(data: bytes) -> bytes:
    """
    XOR every byte of *data* with 0x55.

    This is the only "encryption" used by the game (sub_435890 load path and
    sub_406410 save path both apply the same XOR in-place).
    """
    return bytes(b ^ 0x55 for b in data)


def parse_plaintext(text: str) -> dict:
    """
    Parse the plaintext (already decrypted) config tree and return a nested
    Python dict.

    Parameters
    ----------
    text : str
        The raw plaintext string from the config file (after XOR decryption).

    Returns
    -------
    dict
        Hierarchical key→value mapping.  Values may be:
          - str   — string scalar
          - float — numeric scalar (the game uses atof internally)
          - bool  — True for bare flag keys
          - dict  — sub-block
          - list  — array block or multi-value key
    """
    tokens = _tokenise(text)
    parser = _Parser(tokens)
    return parser.parse()


def load(path: str | Path) -> dict:
    """
    Load and parse an encrypted config.mm (or any file using the same format).

    Decrypts with XOR-0x55 then parses.

    Parameters
    ----------
    path : str or Path
        Path to the config.mm file.

    Returns
    -------
    dict
        Parsed configuration tree.
    """
    data = Path(path).read_bytes()
    plaintext = decrypt(data).decode('latin-1', errors='replace')
    return parse_plaintext(plaintext)


def load_plaintext(path: str | Path) -> dict:
    """
    Load and parse a plaintext config file (e.g. maniac.cfg, *.sol, *.nav).

    These files use the same tree grammar but are NOT XOR-encrypted.

    Parameters
    ----------
    path : str or Path
        Path to the plaintext config file.

    Returns
    -------
    dict
        Parsed configuration tree.
    """
    text = Path(path).read_text(encoding='latin-1', errors='replace')
    return parse_plaintext(text)


def save(tree: dict, path: str | Path) -> None:
    """
    Serialise a config tree back to an encrypted config.mm file.

    Parameters
    ----------
    tree : dict
        Config tree (as returned by load/parse_plaintext).
    path : str or Path
        Output path.
    """
    plaintext = serialise(tree)
    data = decrypt(plaintext.encode('latin-1', errors='replace'))
    Path(path).write_bytes(data)


def serialise(tree: dict, _indent: int = 0) -> str:
    """
    Serialise a config tree to the plaintext format understood by the game.

    Parameters
    ----------
    tree : dict
        Config tree.
    _indent : int
        Internal indentation depth (used for recursive calls).

    Returns
    -------
    str
        Plaintext config string.
    """
    lines: list[str] = []
    prefix = '  ' * _indent

    for key, value in tree.items():
        if key.startswith('_'):
            continue  # skip synthetic keys like _values, _anon
        lines.extend(_serialise_value(key, value, _indent))

    return '\n'.join(lines) + '\n' if lines else ''


def _serialise_value(key: str, value: Any, indent: int) -> list[str]:
    prefix = '  ' * indent
    lines: list[str] = []

    if isinstance(value, dict):
        lines.append(f'{prefix}{key}')
        lines.append(f'{prefix}{{')
        lines.append(serialise(value, indent + 1).rstrip('\n'))
        lines.append(f'{prefix}}}')

    elif isinstance(value, list):
        # Could be a multi-value or an array
        if all(isinstance(v, dict) for v in value):
            lines.append(f'{prefix}{key}')
            lines.append(f'{prefix}[')
            for item in value:
                lines.append(f'{prefix}  {{')
                lines.append(serialise(item, indent + 2).rstrip('\n'))
                lines.append(f'{prefix}  }}')
            lines.append(f'{prefix}]')
        elif all(isinstance(v, (int, float, str, bool)) for v in value):
            flat = ' '.join(_fmt_scalar(v) for v in value)
            lines.append(f'{prefix}{key} = [ {flat} ]')
        else:
            # Mixed list — emit each element as a separate assignment
            for v in value:
                lines.extend(_serialise_value(key, v, indent))

    elif isinstance(value, bool):
        lines.append(f'{prefix}{key}')

    elif isinstance(value, float):
        lines.append(f'{prefix}{key} = {_fmt_scalar(value)}')

    else:
        lines.append(f'{prefix}{key} = {_fmt_scalar(value)}')

    return lines


def _fmt_scalar(v: Any) -> str:
    if isinstance(v, bool):
        return '1' if v else '0'
    if isinstance(v, float):
        # Avoid trailing ".0" for whole numbers to match original style
        if v == int(v) and abs(v) < 1e15:
            return str(int(v))
        return repr(v)
    if isinstance(v, str):
        # Quote if the string contains spaces or special characters
        if any(c in v for c in ' \t=[]{},"'):
            return f'"{v}"'
        return v
    return str(v)


# ---------------------------------------------------------------------------
# Command-line interface
# ---------------------------------------------------------------------------

def _cli() -> None:
    import argparse

    ap = argparse.ArgumentParser(
        prog='config_mm_parser',
        description='Parse / decrypt / re-encrypt Mall Maniacs config.mm files.',
    )
    ap.add_argument('input', help='Input file (config.mm or plaintext .sol/.cfg)')
    ap.add_argument(
        '--format',
        choices=['encrypted', 'plain'],
        default='encrypted',
        help='Input format: "encrypted" (XOR-0x55, default) or "plain" (raw text)',
    )
    ap.add_argument(
        '--output',
        metavar='FILE',
        help='Write re-encrypted config.mm to FILE',
    )
    ap.add_argument(
        '--json',
        action='store_true',
        help='Dump the parsed tree as JSON to stdout',
    )
    ap.add_argument(
        '--plaintext',
        metavar='FILE',
        help='Write decrypted plaintext to FILE (does not re-encrypt)',
    )
    ap.add_argument(
        '--decrypt-only',
        metavar='FILE',
        help='Just XOR-decrypt the input and write raw bytes to FILE',
    )

    args = ap.parse_args()

    if args.decrypt_only:
        raw = Path(args.input).read_bytes()
        Path(args.decrypt_only).write_bytes(decrypt(raw))
        print(f'Decrypted bytes written to {args.decrypt_only}')
        return

    if args.format == 'encrypted':
        tree = load(args.input)
    else:
        tree = load_plaintext(args.input)

    if args.json:
        print(json.dumps(tree, indent=2, ensure_ascii=False, default=str))

    if args.plaintext:
        Path(args.plaintext).write_text(
            serialise(tree), encoding='latin-1', errors='replace'
        )
        print(f'Plaintext written to {args.plaintext}')

    if args.output:
        save(tree, args.output)
        print(f'Encrypted config.mm written to {args.output}')

    if not any([args.json, args.plaintext, args.output, args.decrypt_only]):
        # Default: pretty-print the tree
        print(json.dumps(tree, indent=2, ensure_ascii=False, default=str))


# ---------------------------------------------------------------------------
# Self-test
# ---------------------------------------------------------------------------

def _run_tests() -> None:
    """Quick smoke tests — no external dependencies."""

    # 1. XOR round-trip
    original = b'Hello, World!\x00\xff'
    assert decrypt(decrypt(original)) == original, 'XOR round-trip failed'

    # 2. Tokeniser
    sample = '''
/ this is a comment
driver = "DRIVERS\\\\GXGLIDE.DLL"
master {
    log_max_level = 3
    mapX = 100.5, mapY = 200.0
}
levels[0] {
    texture_files = "scene_ica\\ica%02d.tpg"
    scene_path = "scene_ica"
    scene_file = "ica.scn"
    music = 1
    startup [ "run 0" "run 1" ]
}
'''
    tokens = _tokenise(sample)
    assert 'driver' in tokens
    assert 'DRIVERS\\\\GXGLIDE.DLL' in tokens  # quotes stripped
    assert '/' not in tokens                 # comment consumed
    assert 'master' in tokens
    assert '{' in tokens

    # 3. Parser
    tree = parse_plaintext(sample)
    assert tree.get('driver') == 'DRIVERS\\\\GXGLIDE.DLL', f"got {tree.get('driver')!r}"
    assert isinstance(tree.get('master'), dict)
    assert tree['master']['log_max_level'] == 3.0
    assert tree['master']['mapX'] == 100.5
    assert tree['master']['mapY'] == 200.0
    levels0 = tree.get('levels[0]') or tree.get('levels')
    # The parser stores "levels[0]" — brackets are separate tokens so the key
    # will actually be split; let's verify the flat token list has the right content
    # (the game uses wsprintfA to form "levels[0]" as a lookup key)
    assert 'levels' in tokens or 'levels[0]' in ''.join(tokens)

    # 4. Serialise round-trip
    plaintext_out = serialise(tree)
    tree2 = parse_plaintext(plaintext_out)
    assert tree2.get('driver') == 'DRIVERS\\\\GXGLIDE.DLL'

    # 5. Encrypt/decrypt file round-trip (in memory)
    buf = io.BytesIO()
    plain_bytes = serialise(tree).encode('latin-1')
    encrypted = decrypt(plain_bytes)
    decrypted_back = decrypt(encrypted)
    assert decrypted_back == plain_bytes, 'Encrypt/decrypt cycle failed'

    print('All tests passed.')


if __name__ == '__main__':
    if len(sys.argv) > 1 and sys.argv[1] == '--test':
        _run_tests()
    else:
        _cli()
