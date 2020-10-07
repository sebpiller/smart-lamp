package ch.sebpiller.iot.lamp.impl;

import static org.junit.Assert.assertEquals;

import ch.sebpiller.iot.lamp.SmartLampFacade;

import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;

public class CompositeLampFacadeTest {
    @Test
    public void testConstructor() {
        // Arrange
        LoggingLamp loggingLamp = new LoggingLamp();
        LoggingLamp loggingLamp1 = new LoggingLamp();
        SmartLampFacade[] smartLampFacadeArray = new SmartLampFacade[]{loggingLamp, loggingLamp1, new LoggingLamp()};

        // Act
        new CompositeLampFacade(smartLampFacadeArray);

        // Assert
        assertEquals(3, smartLampFacadeArray.length);
    }

    @Test
    public void testFrom() {
        // Arrange
        Assert.fail("This is a boilerplate test. Please fix.");
        SmartLampFacade[] composites = null;

        // Act
        CompositeLampFacade actualObject = CompositeLampFacade.from(composites);

        // Assert
        Assert.assertNotNull("This is a boilerplate assert on the result.", actualObject);
    }

    @Test
    public void testPower() {
        // Arrange
        Assert.fail("This is a boilerplate test. Please fix.");
        CompositeLampFacade compositeLampFacade = null;
        boolean on = false;

        // Act
        CompositeLampFacade actualObject = compositeLampFacade.power(on);

        // Assert
        Assert.assertNotNull("This is a boilerplate assert on the result.", actualObject);
    }

    @Test
    public void testFadeBrightnessFromTo() {
        // Arrange
        Assert.fail("This is a boilerplate test. Please fix.");
        CompositeLampFacade compositeLampFacade = null;
        byte from = (byte) 0;
        byte to = (byte) 0;
        SmartLampFacade.FadeStyle fadeStyle = SmartLampFacade.FadeStyle.FAST;

        // Act
        Future<CompositeLampFacade> actualObject = compositeLampFacade.fadeBrightnessFromTo(from, to, fadeStyle);

        // Assert
        Assert.assertNotNull("This is a boilerplate assert on the result.", actualObject);
    }

    @Test
    public void testFadeBrightnessTo() {
        // Arrange
        Assert.fail("This is a boilerplate test. Please fix.");
        CompositeLampFacade compositeLampFacade = null;
        byte percent = (byte) 0;
        SmartLampFacade.FadeStyle fadeStyle = SmartLampFacade.FadeStyle.FAST;

        // Act
        Future<CompositeLampFacade> actualObject = compositeLampFacade.fadeBrightnessTo(percent, fadeStyle);

        // Assert
        Assert.assertNotNull("This is a boilerplate assert on the result.", actualObject);
    }

    @Test
    public void testSetTemperature() throws UnsupportedOperationException {
        // Arrange
        Assert.fail("This is a boilerplate test. Please fix.");
        CompositeLampFacade compositeLampFacade = null;
        int kelvin = 0;

        // Act
        CompositeLampFacade actualObject = compositeLampFacade.setTemperature(kelvin);

        // Assert
        Assert.assertNotNull("This is a boilerplate assert on the result.", actualObject);
    }

    @Test
    public void testFadeTemperatureFromTo() {
        // Arrange
        Assert.fail("This is a boilerplate test. Please fix.");
        CompositeLampFacade compositeLampFacade = null;
        int from = 0;
        int to = 0;
        SmartLampFacade.FadeStyle fadeStyle = SmartLampFacade.FadeStyle.FAST;

        // Act
        Future<CompositeLampFacade> actualObject = compositeLampFacade.fadeTemperatureFromTo(from, to, fadeStyle);

        // Assert
        Assert.assertNotNull("This is a boilerplate assert on the result.", actualObject);
    }

    @Test
    public void testFadeTemperatureTo() {
        // Arrange
        Assert.fail("This is a boilerplate test. Please fix.");
        CompositeLampFacade compositeLampFacade = null;
        int kelvin = 0;
        SmartLampFacade.FadeStyle fadeStyle = SmartLampFacade.FadeStyle.FAST;

        // Act
        Future<CompositeLampFacade> actualObject = compositeLampFacade.fadeTemperatureTo(kelvin, fadeStyle);

        // Assert
        Assert.assertNotNull("This is a boilerplate assert on the result.", actualObject);
    }

    @Test
    public void testFadeColorFromTo() {
        // Arrange
        Assert.fail("This is a boilerplate test. Please fix.");
        CompositeLampFacade compositeLampFacade = null;
        int[] from = null;
        int[] to = null;
        SmartLampFacade.FadeStyle fadeStyle = SmartLampFacade.FadeStyle.FAST;

        // Act
        Future<? extends SmartLampFacade> actualObject = compositeLampFacade.fadeColorFromTo(from, to, fadeStyle);

        // Assert
        Assert.assertNotNull("This is a boilerplate assert on the result.", actualObject);
    }

    @Test
    public void testFadeColorTo() {
        // Arrange
        Assert.fail("This is a boilerplate test. Please fix.");
        CompositeLampFacade compositeLampFacade = null;
        int[] to = null;
        SmartLampFacade.FadeStyle fadeStyle = SmartLampFacade.FadeStyle.FAST;

        // Act
        Future<? extends SmartLampFacade> actualObject = compositeLampFacade.fadeColorTo(to, fadeStyle);

        // Assert
        Assert.assertNotNull("This is a boilerplate assert on the result.", actualObject);
    }
}

