package org.apache.drill.synth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

/**
 * Samples lines from a file
 */
public class FileSampler extends FieldSampler {
    private JsonNode data;
    private IntegerSampler index;
    private int skew = Integer.MAX_VALUE;

    public FileSampler() {
    }

    public void setFile(String lookup) throws IOException {
        if (lookup.matches(".*\\.json")) {
            readJsonData(Files.newInputStreamSupplier(new File(lookup)));
        } else {
            List<String> lines = Files.readLines(new File(lookup), Charsets.UTF_8);
            readDelimitedData(lookup, lines);
        }

        setupIndex();
    }

    private void setupIndex() {
        index = new IntegerSampler();
        index.setMin(0);
        index.setMax(data.size());
        if (skew != Integer.MAX_VALUE) {
            index.setSkew(skew);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setResource(String lookup) throws IOException {
        if (lookup.matches(".*\\.json")) {
            readJsonData(Resources.newInputStreamSupplier(Resources.getResource(lookup)));
        } else {
            List<String> lines = Resources.readLines(Resources.getResource(lookup), Charsets.UTF_8);
            readDelimitedData(lookup, lines);
        }

        setupIndex();
    }

    private void readDelimitedData(String lookup, List<String> lines) {
        Splitter splitter;
        if (lookup.matches(".*\\.csv")) {
            splitter = Splitter.on(",");
        } else if (lookup.matches(".*\\.tsv")) {
            splitter = Splitter.on("\t");
        } else {
            throw new IllegalArgumentException("Must have file with .csv, .tsv or .json suffix");
        }

        List<String> names = Lists.newArrayList(splitter.split(lines.get(0)));
        JsonNodeFactory nf = JsonNodeFactory.withExactBigDecimals(false);
        ArrayNode localData = nf.arrayNode();
        for (String line : lines.subList(1, lines.size())) {
            ObjectNode r = nf.objectNode();
            List<String> fields = Lists.newArrayList(splitter.split(line));
            Preconditions.checkState(names.size() == fields.size(), "Wrong number of fields, expected ", names.size(), fields.size());
            Iterator<String> ix = names.iterator();
            for (String field : fields) {
                r.put(ix.next(), field);
            }
            localData.add(r);
        }
        data = localData;
    }

    private void readJsonData(InputSupplier<? extends InputStream> input) throws IOException {
        ObjectMapper om = new ObjectMapper();
        try (InputStream in = input.getInput()) {
            data = om.readTree(in);
        }
    }

    /**
     * Sets the amount of skew.  Skew is added by taking the min of several samples.
     * Setting power = 0 gives uniform distribution, setting it to 5 gives a very
     * heavily skewed distribution.
     * <p/>
     * If you set power to a negative number, the skew is reversed so large values
     * are preferred.
     *
     * @param skew Controls how skewed the distribution is.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void setSkew(int skew) {
        if (index != null) {
            index.setSkew(skew);
        } else {
            this.skew = skew;
        }
    }

    @Override
    public JsonNode sample() {
        return data.get(index.sample().asInt());
    }
}
