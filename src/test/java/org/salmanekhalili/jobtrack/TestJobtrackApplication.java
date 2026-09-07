package org.salmanekhalili.jobtrack;

import org.springframework.boot.SpringApplication;

public class TestJobtrackApplication {

    public static void main(String[] args) {
        SpringApplication.from(JobtrackApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
