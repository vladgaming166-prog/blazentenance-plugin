package net.blazemc.blazentenance.common.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import net.blazemc.blazentenance.common.config.TextFormat;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class MessageFormatterTest {

    @Test
    void legacyAndHexCodesProduceReadableKickMessage() {
        MessageFormatter formatter = new MessageFormatter(TextFormat.LEGACY);
        String plain = PlainTextComponentSerializer.plainText().serialize(
                formatter.lines(List.of("&c&lBLAZEMC NETWORK", "&#FF5555Serverul BlazeMC Network este în lucru!"))
        );
        assertEquals("BLAZEMC NETWORK\nServerul BlazeMC Network este în lucru!", plain);
    }

    @Test
    void stripRemovesFormatting() {
        assertEquals("Blazentenance", MessageFormatter.strip("&8[&cBlazentenance&8]").replace("[", "").replace("]", "").trim());
        assertFalse(MessageFormatter.strip("&cHello").contains("&"));
    }
}
