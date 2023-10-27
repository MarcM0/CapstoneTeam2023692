package com.example.convoassistant

const val NewReflectPrompt =  """The following is an interaction between a user and a person who is confiding in them. Give a SHORT reflection to the that the user may or may not repeat to the person confiding in them. The reflection must be a plausible guess or assumption about the user's underlying emotions, values, or chain of thought. The reflection must be very short. The reflection must be a statement and not a question. Don't always use "it seems like" or "it sounds like" or "you" at the beginning. Don't always use the phrase "important to you" or "important for you". """;

const val OriginalReflectPrompt =  """The following is an interaction between you and a user. You are a therapist and the user is someone having smoking issues. Give a SHORT reflection to the user's response. The reflection must be a plausible guess or assumption about the user's underlying emotions, values, or chain of thought. The reflection must be very short. The reflection must be a statement and not a question. Don't always use "it seems like" or "it sounds like" or "you" at the beginning. Don't always use the phrase "important to you" or "important for you". """;

const val OriginalRatePrompt = """ Decide, in "True" or "False", whether the "reflection" sentence in the following smoking-related conversation is good.
Please refer to the following operational definition of a reflection in the context of Motivational Interviewing (MI):
Reflective listening statements are made by the clinician in response to client statements. A reflection may introduce new meaning or material, but it essentially captures and returns to clients something about what they have just said. Reflections are further categorized as simple or complex reflections.
Simple reflections typically convey understanding or facilitate client–clinician exchanges. These reflections add little or no meaning (or emphasis) to what clients have said. Simple reflections may mark very important or intense client emotions, but do not go far beyond the client’s original intent in the statement.
Complex reflections typically add substantial meaning or emphasis to what the client has said. These reflections serve the purpose of conveying a deeper or more complex picture of what the client has said. Sometimes the clinician may choose to emphasize a particular part of what the client has said to make a point or take the conversation in a different direction. Clinicians may add subtle or very obvious content to the client’s words, or they may combine statements from the client to form complex summaries.
Here are some additional hard constraints for a reflection to be good:
A reflection must be a statement rather than a question.
A reflection must not be MI-inconsistent in the following ways: Confronting the person by disagreeing, arguing, correcting, shaming, blaming, criticizing, labeling, ridiculing, or questioning the person’s honesty, or directing the person by giving orders, commands, or imperatives, or otherwise challenging the person’s autonomy.
A reflection must not move people to the wrong direction in terms of smoking cessation. If the client has expressed their will towards quitting smoking, do not overstate their statement; if the client has expressed their will against quitting smoking, do not understate their statement.
A reflection must not be factually wrong about smoking.
A reflection must be grammatically correct.
A reflection must be relevant to the conversation.
Given all the context above, please make an informed decision on whether or not the reflection is good. If the reflection is good, output "True". Otherwise, output "False", and output an explanation that includes which properties it has that makes it not good.
""";