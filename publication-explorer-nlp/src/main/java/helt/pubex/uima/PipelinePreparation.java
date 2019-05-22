package helt.pubex.uima;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_NOUN;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.io.pdf.PdfReader;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.mallet.MalletModelTrainer;
import de.tudarmstadt.ukp.dkpro.core.mallet.lda.MalletLdaTopicModelInferencer;
import de.tudarmstadt.ukp.dkpro.core.mallet.lda.MalletLdaTopicModelTrainer;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

class PipelinePreparation {
    private static final Logger LOG = LoggerFactory.getLogger(PipelinePreparation.class);
    private Path dir;
    private Boolean runInTrainingMode;
    private String language = "en";

    public PipelinePreparation(Path dir, boolean runInTrainingMode, String language) {
        this(dir, runInTrainingMode);
        this.language = language;
    }

    public PipelinePreparation(Path dir, boolean b) {
        this.dir = dir;
        this.runInTrainingMode = b;

    }

    public Boolean getRunInTrainingMode() {
        return runInTrainingMode;
    }

    public void runPipeline() throws IOException, UIMAException {
        for (JCas jCas : getPipelineIterator()) {
            // No need to do anything. Just iterate over the documents.
            // Processing happens in the iterator itself.
        }
    }

    public JCasIterable getPipelineIterator() throws IOException, ResourceInitializationException {
        // if the topic model doesnt exist, then we need to run it first.
        File topicModelLocation = new File("data/models/model.mallet");
        if (!topicModelLocation.exists() && !topicModelLocation.canRead()) {
            this.runInTrainingMode = true;
            LOG.info("No topic model existing. Forcing topicModel Training");
        }

        CollectionReaderDescription crd = inferReader(dir);
        AnalysisEngineDescription tokenizer = buildTokenizer(Optional.of(language));

        AnalysisEngineDescription posTagger = createEngineDescription(StanfordPosTagger.class);
        AnalysisEngineDescription nerTagger = createEngineDescription(StanfordNamedEntityRecognizer.class);

        AnalysisEngineDescription ldaTrainer = createEngineDescription(MalletLdaTopicModelTrainer.class,
                JCasFileWriter_ImplBase.PARAM_TARGET_LOCATION, topicModelLocation,
                JCasFileWriter_ImplBase.PARAM_OVERWRITE, true,
                MalletModelTrainer.PARAM_TOKEN_FEATURE_PATH, POS_NOUN.class,
                MalletLdaTopicModelTrainer.PARAM_DISPLAY_N_TOPIC_WORDS, 30,
                MalletModelTrainer.PARAM_COVERING_ANNOTATION_TYPE, Paragraph.class);

        AnalysisEngineDescription ldaInferencer = createEngineDescription(MalletLdaTopicModelInferencer.class,
                MalletLdaTopicModelInferencer.PARAM_MODEL_LOCATION, topicModelLocation,
                MalletLdaTopicModelInferencer.PARAM_TYPE_NAME, POS_NOUN.class,
                MalletLdaTopicModelInferencer.PARAM_LOWERCASE, true);

        JCasIterable iterator = SimplePipeline.iteratePipeline(crd, tokenizer, posTagger, nerTagger, (runInTrainingMode) ? ldaTrainer : ldaInferencer);
        return iterator;
    }

    private AnalysisEngineDescription buildTokenizer(Optional<String> language) throws ResourceInitializationException {

        switch (language.orElse(this.language)) {
            case "ar":
            case "en":
            case "fr":
            case "es":
                return createEngineDescription(StanfordSegmenter.class);
            default:
                return createEngineDescription(OpenNlpSegmenter.class);
        }
    }

    private CollectionReaderDescription inferReader(Path directory) throws IOException, ResourceInitializationException {

        List<String> fileEnding = new ArrayList<>();

        FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.isRegularFile(file)) {

                    if (file.getFileName().endsWith("pdf")) {
                        fileEnding.add("application/pdf");
                    } else if (file.getFileName().endsWith("txt")) {
                        fileEnding.add("test/pain");
                    } else {
                        fileEnding.add(Files.probeContentType(file));
                    }
                }

                return super.visitFile(file, attrs);
            }
        };
        Files.walkFileTree(directory, visitor);

        long arAllPdf = fileEnding.stream().filter(x -> Objects.equals(x, "application/pdf")).count();
        long areAllText = fileEnding.stream().filter(x -> Objects.equals(x, "text/plain")).count();

        if (arAllPdf > areAllText) {
            LOG.info("Using PdfReader");
            return createReaderDescription(PdfReader.class,
                    ResourceCollectionReaderBase.PARAM_LANGUAGE, language,
                    ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION, directory.toString(),
                    ResourceCollectionReaderBase.PARAM_PATTERNS, "[+]/**/*.pdf");
        } else {
            LOG.info("Using TextReader");
            return createReaderDescription(TextReader.class,
                    ResourceCollectionReaderBase.PARAM_LANGUAGE, language,
                    ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION, directory.toString(),
                    ResourceCollectionReaderBase.PARAM_PATTERNS, "[+]/**/*.txt");
        }
    }

}
