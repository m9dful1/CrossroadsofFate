package com.spiritwisestudios.crossroadsoffate.data.models

            import com.google.gson.*
            import java.lang.reflect.Type

            /**
             * Custom deserializer for the LeadsTo sealed class.
             * Handles conversion of JSON data into LeadsTo objects for scenario transitions.
             */
            class LeadsToDeserializer : JsonDeserializer<LeadsTo> {

                /**
                 * Deserializes JSON elements into LeadsTo objects.
                 *
                 * @param json The JSON element to deserialize
                 * @param typeOfT The type of the desired object
                 * @param context The deserialization context
                 * @return LeadsTo object (either Simple or Conditional)
                 * @throws JsonParseException if the JSON format is invalid
                 */
                override fun deserialize(json: JsonElement?, typeOfT: Type, context: JsonDeserializationContext): LeadsTo {
                    return when {
                        // Throw exception if JSON is null
                        json == null -> throw JsonParseException("LeadsTo cannot be null")

                        // Handle simple string case - creates a Simple LeadsTo with just a scenario ID
                        json.isJsonPrimitive -> {
                            LeadsTo.Simple(json.asString)
                        }

                        // Handle object case - could be either Simple or Conditional
                        json.isJsonObject -> {
                            val jsonObject = json.asJsonObject

                            // Check if it's a Simple LeadsTo in object format
                            if (!jsonObject.has("ifConditionMet") && !jsonObject.has("ifConditionNotMet")) {
                                // Extract scenarioId for Simple LeadsTo
                                val scenarioId = jsonObject.get("scenarioId")?.asString
                                    ?: throw JsonParseException("scenarioId is required for Simple LeadsTo")
                                LeadsTo.Simple(scenarioId)
                            } else {
                                // Handle Conditional LeadsTo - requires both condition paths
                                val ifConditionMet = jsonObject.get("ifConditionMet")?.asString
                                    ?: throw JsonParseException("ifConditionMet is required for Conditional LeadsTo")
                                val ifConditionNotMet = jsonObject.get("ifConditionNotMet")?.asString
                                    ?: throw JsonParseException("ifConditionNotMet is required for Conditional LeadsTo")
                                LeadsTo.Conditional(ifConditionMet, ifConditionNotMet)
                            }
                        }

                        // Throw exception for any other JSON format
                        else -> throw JsonParseException("Invalid LeadsTo format")
                    }
                }
            }