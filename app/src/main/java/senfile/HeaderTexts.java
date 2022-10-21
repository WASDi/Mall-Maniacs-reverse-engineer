package senfile;

public class HeaderTexts {

    public static final int REV2 = 844514642; // ok

    public static final int MESH = 1213416781; // ok, points at SUBO
    public static final int NAME = 1162690894; // ok

    public static final int COLS = 1397509955; // värdelöst?

    public static final int MAPI = 1229996365; // Bounding box mot bildfil

    public static final int SUBO = 1329747283; // connects vertices with MAPI?

    public static final int TANI = 1229865300;

    public static final int OBJI = 1229603407; // XYZ pos för mall items, lustigt format

    public static final int TNAM = 1296125524; // namnlista
    public static final int ONAM = 1296125519; // namnlista


    // Om man tar bort...
    // PHMALL-fil: Kollisioner funkar inte
    // MALL-fil: Inget renderas
    // ica.eo: Varor försvinner, och raketen funkar inte
    // ica.ai: crash... MEN det sker EN frames rendering av karta. Så föremåls positioner ligger INTE häri.
    // Av att byta ut denna blir det sporadiskt svart. Den verkar alltså definiera avdelningar och optimera renderingar

    // OM man kopierar in wood-saker i ica-mappen OCH byter namn på eo/ai
    // SÅ renderas den korrekt och varor är på sin plats
    // vad som inte stämmer är plats för spawn, mcdman och kassörska

}
