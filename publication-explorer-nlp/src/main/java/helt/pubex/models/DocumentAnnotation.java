package helt.pubex.models;

import java.util.Objects;
import java.util.UUID;

public class DocumentAnnotation {

    String id;
    Long start;
    Long end;
    String coveredText;
    String type;
    String docType = "annotation";
    // Refers to the content-array of the Document which contains this annotation.
    Integer contentIndex;

    public DocumentAnnotation() {
        // for the pojos
    }

    public DocumentAnnotation(Builder builder) {
        if (builder.id == null) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = builder.id;
        }
        this.start = builder.start;
        this.end = builder.end;
        this.coveredText = builder.content.replaceAll("\\s+", " ").trim();
        this.type = builder.type.replaceAll("\\s+", " ").trim();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public Long getStart() {
        return start;
    }

    public Long getEnd() {
        return end;
    }

    public String getCoveredText() {
        return coveredText;
    }

    public String getType() {
        return type;
    }

    public String getDocType() {
        return docType;
    }

    public Integer getContentIndex() {
        return contentIndex;
    }

    @Override
    public String toString() {
        return "DocumentAnnotation [" + coveredText + ", " + type + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DocumentAnnotation article = (DocumentAnnotation) o;

        if (id != null || article.id != null) {
            return Objects.equals(id, article.id);
        } else {
            return Objects.equals(this.coveredText, article.coveredText)
                    && Objects.equals(type, article.type)
                    && Objects.equals(start, article.start)
                    && Objects.equals(end, article.end)
                    && Objects.equals(this.contentIndex, article.contentIndex);
        }

    }

    @Override
    public int hashCode() {
        return Objects.hash(id, coveredText, type, start, end, contentIndex);
    }

    public static class Builder {

        private String id = UUID.randomUUID().toString();
        private String type;
        private String content;
        private long start = Long.MAX_VALUE;
        private long end = Long.MIN_VALUE;

        Builder() {
        }

        public Builder id(String id) {
            this.id = Objects.requireNonNull(id);
            return this;
        }

        public Builder extent(int begin, int end) {
            this.start = begin;
            this.end = end;
            return this;
        }

        public Builder type(String value) {
            this.type = value;
            return this;
        }

        public Builder content(String value) {
            this.content = value;
            return this;
        }

        public DocumentAnnotation create() {
            return new DocumentAnnotation(this);
        }
    }
}
