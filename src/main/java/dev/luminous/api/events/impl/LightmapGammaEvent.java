package dev.luminous.api.events.impl;


import dev.luminous.api.events.Event;

/**
 * @author NRC4
 * @since 1.21.11+build
 */
public class LightmapGammaEvent extends Event
{
    //
    private float gamma;

    /**
     * @param gamma
     */
    public LightmapGammaEvent(float gamma)
    {
        this.gamma = gamma;
    }

    public float getGamma()
    {
        return gamma;
    }

    public void setGamma(int gamma)
    {
        this.gamma = gamma;
    }
}
