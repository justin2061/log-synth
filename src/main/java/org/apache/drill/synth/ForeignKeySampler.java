package org.apache.drill.synth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.google.common.base.Preconditions;
import org.apache.mahout.math.random.Multinomial;

/**
 * Samples from a "foreign key" which is really just an integer.
 * <p/>
 * The only cleverness here is that we allow a variable amount of key skew.
 */
@JsonIgnoreProperties({"base"})
public class ForeignKeySampler extends FieldSampler {
    private int size = 1000;
    private double skew = 0.5;

    private Multinomial<Integer> base;

    public ForeignKeySampler() {
    }

    public ForeignKeySampler(int size, double skew) {
        setSize(size);
        setSkew(skew);
    }

    public void setSize(int size) {
        Preconditions.checkArgument(size > 0);
        this.size = size;

        setup();
    }

    public void setSkew(double skew) {
        Preconditions.checkArgument(skew >= 0, "Skew should be non-negative");
        Preconditions.checkArgument(skew <= 3, "Skew should be less than or equal to 3");

        this.skew = skew;

        setup();
    }

    private void setup() {
        base = new Multinomial<>();
        for (int i = 0; i < size; i++) {
            base.add(i, Math.pow(i + 1.0, -skew));
        }
    }

    @Override
    public JsonNode sample() {
        return new IntNode(base.sample());
    }
}
