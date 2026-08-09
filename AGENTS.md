# AGENTS.md

Reverse engineering of the game Mall Maniacs. Parses and renders `.SEN`, `.TPG`, and `.ANM` files from the game data.

## Layout

- `app/` — Maven module with all Java code (`app/src/main/java`).
- Root `pom.xml` — parent POM; modules: `app`. Java release 21.
- Game data lives at `/Users/wasd/Downloads/Mall Maniacs/` (`senfile/Util.ROOT_DIR`, hardcoded).

## Main packages (`app/src/main/java`)

- `senfile` — `.SEN` parsing: `SenFile`, `SenFileFactory`, `GameMap`, `Util`. Parts under `senfile/parts` (e.g. `Mapi`, `Subo`, `Obji`, `Cols`, `Tnam`, `Onam`, `Tani`), element records under `senfile/parts/elements`, mesh types under `senfile/parts/mesh` (`MeshObject`, `MeshCharacter`, `SenMesh`).
- `senfile/serializer` — serializes `.SEN` back to bytes.
- `senfile/factories` — per-section parsers.
- `render` — mesh drawing: `SenMeshRenderer` (fixed-pipeline face drawing + texture init, shared by all renderers); LWJGL GLFW rendering of `.SEN` files.
- `tpgviewer` — Swing viewer for `.TPG` textures.
- `anmfile` — `.ANM` animation parsing.
- `senviewer` — Swing viewer for `.SEN` files (GameMap + file dropdowns, `JTree` browse, `JTable` of properties, `MeshViewPanel` with an embedded OpenGL canvas via `org.lwjglx:lwjgl3-awt`). Main: `senviewer.SenViewerMain`.
- `org/lwjglb/...` — LWJGL boilerplate.

## Conventions

- Public mutable fields instead of getters; small immutable-ish classes built from a `ByteBuffer`.
- All data is little-endian (`ByteOrder.LITTLE_ENDIAN`).
- Comments are minimal; code contains reverse-engineering notes (observed constant values, etc.).
- JUnit 4 tests under `app/src/test/java`.

## Build

- Maven: `mvn -q -pl app compile`.
