package us.wearecurio.jobs

import us.wearecurio.model.PasswordRecovery


class CleanupPasswordRecoveryJob extends us.wearecurio.utility.TimerJob {
    static triggers = {
		simple startDelay: DAY, repeatInterval: DAY
    }

    def execute() {
        PasswordRecovery.deleteStaleRecoveries()
    }
}
