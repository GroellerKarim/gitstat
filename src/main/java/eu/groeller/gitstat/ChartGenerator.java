package eu.groeller.gitstat;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChartGenerator {
    private final Configuration cfg;
    
    public ChartGenerator() {
        cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassLoaderForTemplateLoading(
            ChartGenerator.class.getClassLoader(), "templates");
    }

    public void generateCharts(List<DateCommitRecord> timeSeriesData,
                             Path outputPath) throws IOException, TemplateException {
        Map<String, Object> data = new HashMap<>();
        
        // Convert data to JSON arrays
        String datesJson = timeSeriesData.stream()
            .map(r -> "\"" + r.date().atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_LOCAL_DATE) + "\"")
            .collect(Collectors.joining(", ", "[", "]"));
        
        String commitsJson = timeSeriesData.stream()
            .map(r -> String.valueOf(r.commitCount()))
            .collect(Collectors.joining(", ", "[", "]"));
        
        String additionsJson = timeSeriesData.stream()
            .map(r -> String.valueOf(r.additions()))
            .collect(Collectors.joining(", ", "[", "]"));
        
        String deletionsJson = timeSeriesData.stream()
            .map(r -> String.valueOf(r.deletions()))
            .collect(Collectors.joining(", ", "[", "]"));

        data.put("dates", datesJson);
        data.put("commits", commitsJson);
        data.put("additions", additionsJson);
        data.put("deletions", deletionsJson);

        Template template = cfg.getTemplate("charts.ftl");
        try (Writer writer = Files.newBufferedWriter(outputPath)) {
            template.process(data, writer);
        }
    }
} 