package helt.pubex.uima;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_NOUN;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Heading;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.mallet.type.TopicDistribution;
import helt.pubex.models.Document;
import helt.pubex.models.DocumentAnnotation;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class UimaNlpTopicModellingService {

	private String outputDirectory = "data/outputDirectory";
	private static final String DEFAULT_LANGUAGE = "en";
	private final ObjectMapper om = new ObjectMapper();
	private static final Logger LOG = LoggerFactory.getLogger(UimaNlpTopicModellingService.class);

	/**
	 * This method derives global information of a data directory. E.g. it trains a
	 * topic model or could calculate the tf-idf information.
	 * 
	 * @param inputDirectory
	 */
	public void preprocessDirectory(Path inputDirectory) throws IOException, UIMAException {
		Path dir = Objects.requireNonNull(inputDirectory);

		if (!Files.isDirectory(dir)) {
			dir = dir.getParent();
		}
		PipelinePreparation pipelinePreparation = new PipelinePreparation(dir, true);
		pipelinePreparation.runPipeline();
	}

	public void processDirectory(Path inputDirectory) throws IOException, UIMAException {
		Path dir = Objects.requireNonNull(inputDirectory);

		if (!Files.isDirectory(dir)) {
			dir = dir.getParent();
		}
		PipelinePreparation pipelinePreparation = new PipelinePreparation(dir, false);

		int i = 0;
		int successes = 0;
		int failures = 0;
		for (JCas jcas : pipelinePreparation.getPipelineIterator()) {
			i++;
			Document d = postProcessDocument(i, jcas);

			LOG.info("Finished with document #" + i);
		}
		LOG.info("End of Fahnenstange");

	}

	private Document postProcessDocument(int i, JCas jcas) {
		try {
			Collection<DocumentAnnotation> documentAnnotations = extractNounPhrases(jcas);
			// Collection<DocumentAnnotation> headings = extractHeadings(jcas);
			Collection<DocumentAnnotation> namedEntities = extractNamedEntities(jcas);
			Collection<DocumentMetaData> metaData = extractMetaData(jcas);
			List<String> paragraphs = JCasUtil.select(jcas, Paragraph.class).stream().map(Annotation::getCoveredText)
					.collect(Collectors.toList());

			JCasUtil.select(jcas, TopicDistribution.class).stream().forEach(td -> {
				IntegerArray ia = td.getTopicAssignment();
				LOG.info(String.format("Assignments: %s, proportions: %s", td.getTopicAssignment(),
						td.getTopicProportions()));
			});

			Document document = Document.builder().setLanguage(jcas.getDocumentLanguage()).setContent(paragraphs)
					.addAnnotations(documentAnnotations)
					// .addAnnotations(headings)
					.addAnnotations(namedEntities).setUrl(metaData.stream().map(DocumentMetaData::getDocumentUri)
							.findFirst().orElse("file://unknown"))
					.setId(UUID.randomUUID().toString()).create();
			// storeDocument(document);
			return document;

		} catch (Exception oO) {
			LOG.error(String.format("Error parsing file %s", i), oO);
		}
		return null;
	}

	private void storeDocument(Document document) {
		try {
			Path targetDir = Paths.get(getOutputDirectory());
			Files.createDirectories(targetDir);

			Path tempFile = Files.createFile(Paths.get(targetDir.toString(), document.getId() + ".json"));
			om.writeValue(Files.newBufferedWriter(tempFile), document);
		} catch (IOException e) {
			LOG.error("Error while saving files", e);
		}
	}

	private Collection<DocumentAnnotation> extractNamedEntities(JCas aJCas) {
		return JCasUtil
				.select(aJCas, NamedEntity.class).stream().map(x -> DocumentAnnotation.builder()
						.extent(x.getBegin(), x.getEnd()).content(x.getCoveredText()).type(x.getValue()).create())
				.collect(Collectors.toList());
	}

	private Collection<DocumentMetaData> extractMetaData(JCas aJCas) {
		return JCasUtil.select(aJCas, DocumentMetaData.class);
	}

	private Collection<DocumentAnnotation> extractHeadings(JCas aJCas) {
		return JCasUtil
				.select(aJCas, Heading.class).stream().map(x -> DocumentAnnotation.builder().content(x.getCoveredText())
						.extent(x.getBegin(), x.getEnd()).type(Heading.class.getSimpleName()).create())
				.collect(Collectors.toList());
	}

	private Collection<DocumentAnnotation> extractNounPhrases(JCas jCas) {
		Collection<POS_NOUN> properNouns = JCasUtil.select(jCas, POS_NOUN.class);
		Collection<DocumentAnnotation> annotations = new ArrayList<>();

		for (POS_NOUN n : properNouns) {
			DocumentAnnotation.Builder ab = DocumentAnnotation.builder();
			DocumentAnnotation annotation = ab.extent(n.getBegin(), n.getEnd()).content(n.getCoveredText())
					.type(n.getType().getShortName()).create();
			annotations.add(annotation);
		}

		return annotations;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

}
