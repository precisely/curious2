package us.wearecurio.thirdparty

import grails.test.spock.IntegrationSpec
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.JawboneUpDataService
import us.wearecurio.services.MovesDataService
import us.wearecurio.services.LegacyOuraDataService
import us.wearecurio.services.WithingsDataService

class TagUnitMapIntegrationSpec extends IntegrationSpec {

    void "test theSourceSetIdentifiers map"() {
        expect: "Following lines should be true"
        TagUnitMap.theSourceSetIdentifiers[WithingsDataService.SET_NAME] == WithingsDataService.SOURCE_NAME
        TagUnitMap.theSourceSetIdentifiers[FitBitDataService.SET_NAME] == FitBitDataService.SOURCE_NAME
        TagUnitMap.theSourceSetIdentifiers[MovesDataService.SET_NAME] == MovesDataService.SOURCE_NAME
        TagUnitMap.theSourceSetIdentifiers[JawboneUpDataService.SET_NAME] == JawboneUpDataService.SOURCE_NAME
        TagUnitMap.theSourceSetIdentifiers[LegacyOuraDataService.SET_NAME] == LegacyOuraDataService.SOURCE_NAME
    }

    void "test setIdentifierToSource method"() {
        expect: "Following lines should be true"
        TagUnitMap.setIdentifierToSource("moves import") == "Moves Data"
        TagUnitMap.setIdentifierToSource("moves import 03-16-2016") == "Moves Data"
        TagUnitMap.setIdentifierToSource("WIa1376820000") == "Withings Data"
        TagUnitMap.setIdentifierToSource("OURAac2015-12-22 00:00:00.0-1") == "Oura Data"
        TagUnitMap.setIdentifierToSource("fitbit import 2015-08-20") == "FitBit Data"
        TagUnitMap.setIdentifierToSource("JUP 2015-08-20") == "Jawbone Up Data"
    }
}