package com.codebrig.phenomena

import grakn.client.GraknClient
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.fail

class PhenomenaTest {

    @Before
    void setupGrakn() {
        try (def graknClient = GraknClient.core("172.19.0.1:1729")) {
            if (graknClient.databases().contains("grakn")) {
                graknClient.databases().delete("grakn")
            }
            graknClient.databases().create("grakn")
        }
    }

    @Test
    void testInvalidBabelfishConnection() {
        def phenomena = new Phenomena()
        phenomena.babelfishPort = 1000

        try {
            phenomena.connectToBabelfish()
            fail("Expected a ConnectionException to be thrown")
        } catch (ConnectionException ex) {
        }
    }

    @Test
    void testInvalidGraknConnection() {
        def phenomena = new Phenomena()
        phenomena.graknPort = 1000

        try {
            phenomena.connectToGrakn()
            fail("Expected a ConnectionException to be thrown")
        } catch (ConnectionException ex) {
        }
    }
}