package org.tbk.tor;

import org.berndpruenster.netlayer.tor.NativeTor;
import org.junit.Before;
import org.junit.Test;

public class NativeTorFactoryTest {
    // "www.torproject.org" as onion. taken from https://onion.torproject.org/ on 2020-01-13
    private static final String onionUrl = "expyuzz" + "4wqqyqh" + "j" + "n.on" + "ion";

    NativeTorFactory sut;

    @Before
    public void setUp() {
        this.sut = new NativeTorFactory();
    }

    @Test
    public void itShouldCreateTorSuccessfully() {
        NativeTor nativeTor = sut.create();

        nativeTor.shutdown();
    }
}