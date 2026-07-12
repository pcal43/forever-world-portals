package net.pcal.fwportals.portal;

import net.minecraft.resources.Identifier;
import net.pcal.fwportals.attunement.AttunementDefinition;

public final class PortalFeedbackText {

    private PortalFeedbackText() {
    }

    public static String readableAttunementName(String attunementId) {
        String path = attunementId;
        int namespaceSeparator = path.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < path.length()) {
            path = path.substring(namespaceSeparator + 1);
        }

        String[] words = path.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.isEmpty() ? attunementId : builder.toString();
    }

    public static String readableAttunementName(AttunementDefinition attunementDefinition) {
        return readableAttunementName(attunementDefinition.id());
    }

    public static String readableAttunementName(Identifier attunementItemId) {
        return readableAttunementName(attunementItemId.toString());
    }

    public static String acceptedAttunementMessage(AttunementDefinition attunementDefinition) {
        return "Portal attuned to " + readableAttunementName(attunementDefinition);
    }

    public static String seekingFrontierMessage(AttunementDefinition attunementDefinition) {
        if ("default".equals(attunementDefinition.id())) {
            return "Seeking a new frontier...";
        }
        return "Seeking a " + readableAttunementName(attunementDefinition) + " frontier...";
    }
}
