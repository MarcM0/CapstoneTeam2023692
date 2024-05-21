# Capstone Team 2023692 Wiki

Welcome to the wiki for Capstone Team 2023692, comprising Marc Morcos, Stefan Scodellaro, and James Du. Our project focuses on enhancing reflective listening skills through an Android application named ConvoAssistant. This application is designed to assist users in engaging more effectively in conversations by leveraging advanced AI technologies.

## Application Overview

ConvoAssistant is an innovative Android application developed to improve users' reflective listening skills. Reflective listening is a communication strategy where the listener focuses on understanding their conversation partner and paraphrases their message back to them. This technique helps in gaining deeper insights into the partner’s feelings and improving overall communication quality.

### Features

- **Real-Time Assistant (RTA) Mode:** This mode serves as a live assistant during conversations. It listens to the ongoing dialogue, processes the information, and provides the user with reflective statements to repeat. This is particularly useful for deep and meaningful conversations where there is time to reflect before responding.

- **Practice Mode:** In this mode, the application generates various scenarios for the user to practice reflective listening. The user reflects on these scenarios, and the application evaluates their responses, providing feedback and suggestions for improvement.

### Implementation

- **Large Language Model Integration:** The application uses OpenAI’s GPT-3.5 turbo model to generate reflective dialogue.
- **Audio Processing:** Audio input is converted to text for the LLM, and the text output is then converted back to audio to be played aloud to the user.
- **Speaker Diarization:** In RTA Mode, the application uses Google Cloud’s Speech diarization platform to distinguish between different speakers in a conversation.


## Project Management

### Gantt Charts

Our project progress is tracked using Gantt charts, which are continually updated to reflect the current status. You can view the Gantt charts at: [Gantt Charts](https://github.com/MarcM0/CapstoneTeam2023692/projects?query=is%3Aopen)

### Wiki

For detailed information on meeting notes, research notes, and other project-related documentation, please refer to our wiki at: [Project Wiki](https://github.com/MarcM0/CapstoneTeam2023692/wiki)
