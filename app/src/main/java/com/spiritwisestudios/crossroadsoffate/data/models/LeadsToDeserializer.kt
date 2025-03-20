package com.spiritwisestudios.crossroadsoffate.data.models

                        import com.google.gson.*
                        import java.lang.reflect.Type

                        class LeadsToDeserializer : JsonDeserializer<LeadsTo> {
                            override fun deserialize(json: JsonElement?, typeOfT: Type, context: JsonDeserializationContext): LeadsTo {
                                return when {
                                    json == null -> throw JsonParseException("LeadsTo cannot be null")
                                    json.isJsonPrimitive -> {
                                        LeadsTo.Simple(json.asString)
                                    }
                                    json.isJsonObject -> {
                                        val jsonObject = json.asJsonObject
                                        // If it's an object but doesn't have conditional fields, treat it as Simple
                                        if (!jsonObject.has("ifConditionMet") && !jsonObject.has("ifConditionNotMet")) {
                                            val scenarioId = jsonObject.get("scenarioId")?.asString
                                                ?: throw JsonParseException("scenarioId is required for Simple LeadsTo")
                                            LeadsTo.Simple(scenarioId)
                                        } else {
                                            // Handle Conditional case
                                            val ifConditionMet = jsonObject.get("ifConditionMet")?.asString
                                                ?: throw JsonParseException("ifConditionMet is required for Conditional LeadsTo")
                                            val ifConditionNotMet = jsonObject.get("ifConditionNotMet")?.asString
                                                ?: throw JsonParseException("ifConditionNotMet is required for Conditional LeadsTo")
                                            LeadsTo.Conditional(ifConditionMet, ifConditionNotMet)
                                        }
                                    }
                                    else -> throw JsonParseException("Invalid LeadsTo format")
                                }
                            }
                        }