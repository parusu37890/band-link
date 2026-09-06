package com.example.bandlink.service;

import com.example.bandlink.dto.ReportRequest;
import com.example.bandlink.entity.*;
import com.example.bandlink.repository.*;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final ReportRepository reports;
    private final UserRepository users;
    private final MessageRepository messages;
    private final Clock clock;

    public ReportService(ReportRepository r, UserRepository u, MessageRepository m) {
        reports = r; users = u; messages = m; clock = Clock.systemDefaultZone();
    }

    public void create(Long reporter, ReportRequest r) {
        User u = users.findById(reporter).orElseThrow();
        Report report = new Report(u, r.targetType(), r.targetId(), r.reason().trim(), LocalDateTime.now(clock));
        // A moderator has to see what was reported, but requirements 8章 limits that to the reported
        // message itself — never the conversation around it. Copying the body and image at report
        // time is what makes that possible: reading the thread later would expose the rest of it,
        // and the message may be gone by the time anyone looks.
        if (r.targetType() == ReportTargetType.MESSAGE) {
            messages.findById(r.targetId())
                    .ifPresent(message -> report.captureSnapshot(message.getContent(), message.getImageUrl()));
        }
        reports.save(report);
    }
}
