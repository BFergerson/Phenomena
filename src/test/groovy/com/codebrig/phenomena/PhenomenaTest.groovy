package com.codebrig.phenomena

import org.junit.Test

import static org.junit.Assert.fail

class PhenomenaTest {

    @Test
    void testInvalidBabelfishConnection() {
        def phenomena = new Phenomena()
        phenomena.babelfishPort = 1000

        try {
            phenomena.connectToBabelfish()
            fail("Expected a ConnectException to be thrown")
        } catch (ConnectException ex) {
        }
    }

    @Test
    void testInvalidGraknConnection() {
        def phenomena = new Phenomena()
        phenomena.graknPort = 1000

        try {
            phenomena.connectToGrakn()
            fail("Expected a ConnectException to be thrown")
        } catch (ConnectException ex) {
        }
    }
}