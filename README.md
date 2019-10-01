# SimpleCanComm

## Introduction

- This Repository is for communication system among Android, IoT, CAN



## SYSTEM ARCH

- System Archtecture

![System_Archtecture](img/System_Archtecture.jpg)

- Android : [ServerIoTCommunicationExercise](/ServerIoTCommunicationExcerise)
  - MainActivity.java
    - ConnectServerTask
      - This Class is for communication task with computer server for test
    - ConnectIoTTask
      - This Class is for communication task with IoT device



- LattePanda : [can](/can)
  - SerialTestS.java
    - SerialWriter
      - Send the data using can bus to IoT device
    - SerialEvent
      - Receive the data using can bus from IoT device
    - Sender
      - Send the data using socket to Android Platform
    - Receiver
      - Receive the data using socket from Android Platform



- LattePanda : [CAN](/CAN)
  - SerialTest.java
    - SerialWriter
      - Send the data using can bus to IoT device
    - SerialEvent
      - Receive the data using can bus from IoT device



## Format

- CAN BUS

  - Send

  ![Can_Send_Format](img/Can_Send_Format.jpg)

  

  - Receive

  ![Can_Receive_Format](img/Can_Receive_Format.jpg)
