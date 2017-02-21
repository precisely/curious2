package us.wearecurio.thirdparty

import grails.test.spock.IntegrationSpec
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.JawboneUpDataService
import us.wearecurio.services.LegacyOuraDataService
import us.wearecurio.services.MovesDataService
import us.wearecurio.services.OuraDataService
import us.wearecurio.services.WithingsDataService

class TagUnitMapIntegrationSpec extends IntegrationSpec {

    void "test theSourceSetIdentifiers map"() {
        expect: "Following lines should be true"
        TagUnitMap.theSourceSetIdentifiers[WithingsDataService.SET_NAME] == WithingsDataService.SOURCE_NAME
        TagUnitMap.theSourceSetIdentifiers[FitBitDataService.SET_NAME] == FitBitDataService.SOURCE_NAME
        TagUnitMap.theSourceSetIdentifiers[MovesDataService.SET_NAME] == MovesDataService.SOURCE_NAME
        TagUnitMap.theSourceSetIdentifiers[JawboneUpDataService.SET_NAME] == JawboneUpDataService.SOURCE_NAME
        TagUnitMap.theSourceSetIdentifiers[OuraDataService.SET_NAME] == OuraDataService.SOURCE_NAME
        TagUnitMap.theSourceSetIdentifiers[LegacyOuraDataService.SET_NAME] == LegacyOuraDataService.SOURCE_NAME
    }

    void "test setIdentifierToSource method"() {
        expect: "Following lines should be true"
        TagUnitMap.setIdentifierToSource("Moves") == "Moves Data"
        TagUnitMap.setIdentifierToSource("Withings") == "Withings Data"
        TagUnitMap.setIdentifierToSource("Oura") == "Oura Data"
        TagUnitMap.setIdentifierToSource("FitBit") == "FitBit Data"
        TagUnitMap.setIdentifierToSource("Jawbone Up") == "Jawbone Up Data"
    }
}