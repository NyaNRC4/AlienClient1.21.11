package satin;

import java.util.List;

public interface SamplerAccess {
   boolean hasSampler(String var1);

   List getSamplerNames();

   List getSamplerShaderLocs();
}
