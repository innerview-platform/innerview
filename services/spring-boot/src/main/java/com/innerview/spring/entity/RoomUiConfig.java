package com.innerview.spring.entity;

import com.innerview.spring.enums.InterviewType;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class RoomUiConfig {
    private boolean showProblemStatement;
    private boolean showSharedEditor;
    private boolean showSystemCanvas;

    public static RoomUiConfig defaultForType(InterviewType interviewType)
    {
        if(interviewType == InterviewType.HR)
            return new RoomUiConfig(true,false,false);
        else if(interviewType == InterviewType.PROBLEM_SOLVING)
            return new RoomUiConfig(true,true,false);
        else if(interviewType == InterviewType.SYSTEM_DESIGN)
            return new RoomUiConfig(true,false,true);
        else // For other types, we can have a default configuration or throw an exception
            return new RoomUiConfig(true,true,true);
    }
}

