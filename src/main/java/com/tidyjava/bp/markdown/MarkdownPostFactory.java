package com.tidyjava.bp.markdown;

import com.tidyjava.bp.post.Post;
import com.tidyjava.bp.post.PostFactory;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor;
import org.commonmark.html.HtmlRenderer;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.tidyjava.bp.util.ExceptionUtils.rethrow;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Service
public class MarkdownPostFactory implements PostFactory {
    private static final String EXTENSION = ".md";

    private static Node parse(File file) {
        Parser parser = Parser.builder().extensions(singletonList(YamlFrontMatterExtension.create())).build();
        return rethrow(() -> {
            try (FileReader input = new FileReader(file)) {
                return parser.parseReader(input);
            }
        });
    }

    private static Map<String, List<String>> extractMetadata(Node document) {
        YamlFrontMatterVisitor visitor = new YamlFrontMatterVisitor();
        document.accept(visitor);
        return visitor.getData();
    }

    private static String getOrDefault(Map<String, List<String>> metadata, String field, String defaultValue) {
        return metadata.getOrDefault(field, asList(defaultValue)).get(0);
    }

    private static List<String> getOrDefault(Map<String, List<String>> metadata, String field, List<String> defaultValue) {
        return metadata.getOrDefault(field, defaultValue);
    }

    private static LocalDate toLocalDate(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static String toUrl(String filename) {
        return "/" + filename.replace(EXTENSION, "");
    }

    private static String toHtml(Node parsedResource) {
        return HtmlRenderer.builder().build().render(parsedResource);
    }

    @Override
    public String extension() {
        return EXTENSION;
    }

    @Override
    public Post create(File file) {
        Node parsedResource = parse(file);
        Map<String, List<String>> metadata = extractMetadata(parsedResource);

        String title = getOrDefault(metadata, "title", "TILT");
        String summary = getOrDefault(metadata, "summary", "TILT");
        LocalDate date = toLocalDate(getOrDefault(metadata, "date", "1970-01-01"));
        String url = toUrl(file.getName());
        String content = toHtml(parsedResource);
        List<String> tags = getOrDefault(metadata, "tags", emptyList());

        return new Post(title, summary, date, url, content, tags);
    }
}