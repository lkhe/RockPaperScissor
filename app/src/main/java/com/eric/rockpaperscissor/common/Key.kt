package com.eric.rockpaperscissor.common

class Key {

    companion object {

        private const val publicKey:String = "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAv1S/Nadq0jxwtqlRceBM9xlAq1VOKNSYP1zIBHtX0GJKxwgw/8jxz8bZW81jE1Smbq1DQpq24TGb4S7BmMyLzynNlKiEO06AIsaKuySaVjzbIfP4TzlKw6VYdWSCipja319Or9S77S8mapRiSTwjrXcnANmX4vKTpPRtKSPluyOOb3AUeiNP9Kz3SIlUQH6qI9xSdf2VX1SQA7OCkEH6Pm8gVX31lcx1PMdg8zusZLHIh34T+VM/darEVA5YOS2tQU2X0skbA82hW57QBQ8/OhrWF3jVLfH+TOaAvaoQdotgxNEVF68VVJn0WDLWNVJDIy2DeB/pjies0Yi8sSPRkxj0QbYNmU4LBHcpyrpRf8dshr1NVwk5kMpulsey30eBVIYoE/cCG7ABd/EL0lePYJE3Oc0QATayTozSFaU8ctqJokfMfMyCkU/X1+x2ptLMoeM1IBngohlWRbDRpdyfUyr+5/kImpM7yqbO/3XwxyF15m2wJKNSjrJg91Ui+COJAgMBAAE="


        /**
         * get the publicKey of the application
         * During the encoding process, avoid storing the public key in clear text.
         * @return
         */
        fun getPublicKey() : String {
            return publicKey
        }

    }

}