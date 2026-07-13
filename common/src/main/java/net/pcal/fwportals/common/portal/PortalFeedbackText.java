package net.pcal.fwportals.common.portal;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.pcal.fwportals.common.attunement.AttunementDefinition;

public final class PortalFeedbackText {

    private static final String PREFIX = "message.fwportals.";

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

    public static MutableComponent acceptedAttunementMessage(AttunementDefinition attunementDefinition) {
        String readableName = readableAttunementName(attunementDefinition);
        return Component.translatableWithFallback(
                PREFIX + "attuned",
                "Portal attuned to %s",
                readableName
        );
    }

    public static MutableComponent seekingFrontierMessage(AttunementDefinition attunementDefinition) {
        if ("default".equals(attunementDefinition.id())) {
            return Component.translatableWithFallback(
                    PREFIX + "seeking_new_frontier",
                    "Seeking a new frontier..."
            );
        }
        return Component.translatableWithFallback(
                PREFIX + "seeking_frontier",
                "Seeking a %s frontier...",
                readableAttunementName(attunementDefinition)
        );
    }

    public static MutableComponent inventoryBlockedMessage() {
        return Component.translatableWithFallback(
                PREFIX + "inventory_blocked",
                "You cannot enter a Forever World Portal while carrying items."
        );
    }

    public static MutableComponent foundingInProgressMessage() {
        return Component.translatableWithFallback(
                PREFIX + "founding_in_progress",
                "Forever World portal founding is already in progress."
        );
    }

    public static MutableComponent destinationUnavailableMessage() {
        return Component.translatableWithFallback(
                PREFIX + "destination_unavailable",
                "Forever World destination is unavailable. Try again later."
        );
    }

    public static MutableComponent returnPortalClaimedMessage() {
        return Component.translatableWithFallback(
                PREFIX + "return_portal_claimed",
                "Forever World return portal location is already claimed."
        );
    }

    public static MutableComponent returnPortalFoundingInProgressMessage() {
        return Component.translatableWithFallback(
                PREFIX + "return_portal_in_progress",
                "Forever World return portal founding is already in progress."
        );
    }

    public static MutableComponent foundingFailedMessage() {
        return Component.translatableWithFallback(
                PREFIX + "founding_failed",
                "Forever World portal founding failed. Try again later."
        );
    }

    public static MutableComponent noSafeDestinationMessage() {
        return Component.translatableWithFallback(
                PREFIX + "no_safe_destination",
                "Forever World portal could not find a safe destination yet."
        );
    }

}
