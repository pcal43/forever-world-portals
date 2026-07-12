package net.pcal.fwportals.portal;

import net.pcal.fwportals.attunement.DestinationTargets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortalTravelServiceTest {

    @Test
    void foundingTravelAlwaysUsesDefaultDestinationTarget() {
        assertEquals(DestinationTargets.defaultBiomeTarget(), PortalTravelService.defaultFoundingDestinationTarget());
    }
}
