package com.libera.ticket.config;

import com.libera.ticket.domain.Performer;
import com.libera.ticket.repo.PerformerRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {
    private final PerformerRepo performerRepo;
    @Override public void run(String... args) {
        if(performerRepo.count()==0){
            performerRepo.saveAll(List.of(
                    n("Alice"), n("Bob"), n("Carol"), n("Dave"), n("Eve"),
                    n("Frank"), n("Grace"), n("Heidi"), n("Ivan"), n("Judy"),
                    n("Mallory"), n("Niaj"), n("Olivia"), n("Peggy"), n("Sybil"),
                    n("Trent"), n("Victor"), n("Walter"), n("Yvonne"), n("Zara"),
                    n("Liam"), n("Emma"), n("Noah"), n("Ava"), n("Mia")
            ));
        }
    }
    private Performer n(String s){ var p=new Performer(); p.setName(s); return p; }
}
