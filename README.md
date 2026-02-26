# spring-zkteco-pull-sdk
Spring Boot Starter for ZKTeco Attendance Devices — Connect, fetch, and manage attendance data over TCP (port 4370).

## Installation

Add the following dependency to your `pom.xml`:
```xml
<dependency>
    <groupId>com.hasanjahidul</groupId>
    <artifactId>zkteco-pull-spring-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

For Gradle, add this to your `build.gradle`:
```gradle
implementation 'com.hasanjahidul:zkteco-pull-spring-sdk:1.0.0'
```

## Features

### Connection Management
- Connect / Disconnect and Connection Status Checking

### Device Information
- Fetch Device Info (Name, Serial Number, OS Version, Platform, Time, Firmware Version)
- Check SSR (Self-Service Recorder) Status, Work Code, PIN width, Face recognition feature toggles

### Network Configuration Commands
- Fetch device MAC Address

### User Management
- Fetch All Users
- Add / Update User (Name, Password, Role, Card Number)
- Remove User
- Clear All Users and Clear Admin Roles

### Attendance Management
- Fetch All Attendance Records
- Clear Attendance Logs

### Device Controls & UI Navigation
- Lock / Unlock Terminal
- Remote Unlock Door
- Power Off / Restart
- Sleep / Wake
- Time Sync (Get / Set Time)
- Clear LCD Screen and Write Text to LCD Screen
- Trigger Voice Prompts
