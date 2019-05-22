package helt.pubex.models;

import java.util.*;

public class Document {

    public static final String COLLECTION_NAME = "articles";

    String title;

    String id;
    List<String> content;
    String docType;
    String url;
    String language;
    List<DocumentAnnotation> annotations = new ArrayList<>();

    public Document() {
        // for jackson and other POJO BEAN processors
    }

    public Document(Builder builder) {
        this.docType = "article";
        this.id = builder.id;
        this.title = builder.title;
        this.content = builder.content;
        this.url = builder.url;
        this.language = builder.language;
        this.annotations = builder.annotations;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public List<DocumentAnnotation> getAnnotations() {
        return annotations;
    }

    public String getUrl() {
        return url;
    }

    public String getLanguage() {
        return language;
    }

    public List<String> getContent() {
        return content;
    }

    public static class Builder {
        public String id = null;
        public List<String> content = new ArrayList<>();
        public String url = null;
        public String language = "english";
        public List<DocumentAnnotation> annotations = new ArrayList<>();
        public String title = "This article has no title";

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setContent(List<String> content) {
            this.content = content;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setLanguage(String language) {
            this.language = language;
            return this;
        }

        public Builder setAnnotations(List<DocumentAnnotation> annotations) {
            this.annotations = Objects.requireNonNull(annotations);
            this.annotations.clear();
            return this;
        }

        public Builder addAnnotations(Collection<DocumentAnnotation> annotations) {
            if (annotations == null) {
                return this;
            }

            this.annotations.addAll(annotations);
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Document create() {
            Objects.requireNonNull(id, Document.class.getSimpleName() + " requires non-null id!");
            return new Document(this);
        }

        public Builder addContent(String documentText) {
            return addContent(Collections.singletonList(Objects.requireNonNull(documentText)));
        }

        public Builder addContent(List<String> texts) {
            if (!texts.isEmpty()) {
                this.content.addAll(texts);
            }
            return this;
        }
    }
}
