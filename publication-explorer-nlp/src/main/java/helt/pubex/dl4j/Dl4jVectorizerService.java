package helt.pubex.dl4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.uima.resource.ResourceInitializationException;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.AbstractCache;
import org.deeplearning4j.text.documentiterator.LabelsSource;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.reduce3.EuclideanDistance;
import org.nd4j.linalg.util.MathUtils;

import helt.pubex.Main;
import helt.pubex.uima.SentenceExtractor;

public class Dl4jVectorizerService {

	private final Path input;
	private final Path output;
	private final Path sentencesLocation;

	public Dl4jVectorizerService(Path input, Path output) {
		this.input = Objects.requireNonNull(input);
		this.output = Objects.requireNonNull(output);

		if (!Files.exists(input)) {
			throw new IllegalArgumentException("input does not exist: " + input);
		}

		try {
			Files.createDirectories(this.output);
		} catch (IOException e) {
			throw new IllegalArgumentException("error preparing output", e);
		}

		if (!Files.exists(output)) {
			throw new IllegalArgumentException("output does not exist: " + output);
		}
		sentencesLocation = Paths.get(output.toString(), "sentences.txt");
	}

	/**
	 * 
	 * Uses a UIMA pipeline to extract the sentences from documents, and stores them
	 * on disk. Is required for fitting the paragraph vectors.
	 * 
	 * @throws ResourceInitializationException
	 * @throws IOException
	 */
	public void extractSentencesFromData() throws ResourceInitializationException, IOException {
		SentenceExtractor sentenceExtractor = new SentenceExtractor(input);
		Stream<String> run = sentenceExtractor.run();
		Files.deleteIfExists(sentencesLocation);
		Files.createFile(sentencesLocation);

		BufferedWriter bufferedWriter = Files.newBufferedWriter(sentencesLocation, Charset.defaultCharset());

		run.forEach(x -> {
			try {
				bufferedWriter.append(x);
				bufferedWriter.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		
		bufferedWriter.flush();
		bufferedWriter.close();
	}

	/**
	 * This is WIP for using DL4J to compute Paragraph Vectors. Paragraph Vectors
	 * enable us to compute distances between different documents (or subsets of
	 * documents, like paragraphs).
	 * 
	 * TODO after fitting the vector space, serialize the stuff for later usage.
	 * @throws IOException 
	 * @throws ResourceInitializationException 
	 */
	public void convertSentencesToVectors() throws IOException, ResourceInitializationException {
		Path inputSentences = sentencesLocation;
		if(!Files.exists(sentencesLocation) || Files.size(sentencesLocation) == 0) {
			extractSentencesFromData();
		}

		SentenceIterator iter = new BasicLineIterator(inputSentences.toFile());

		AbstractCache<VocabWord> cache = new AbstractCache<>();

		TokenizerFactory t = new DefaultTokenizerFactory();
		t.setTokenPreProcessor(new CommonPreprocessor());

		/*
		 * if you don't have LabelAwareIterator handy, you can use synchronized labels
		 * generator it will be used to label each document/sequence/line with it's own
		 * label.
		 * 
		 * But if you have LabelAwareIterator ready, you can can provide it, for your
		 * in-house labels
		 */
		LabelsSource source = new LabelsSource("DOC_");

		ParagraphVectors vec = new ParagraphVectors.Builder()
				.minWordFrequency(1).iterations(5).epochs(1).layerSize(100)
				.learningRate(0.025).labelsSource(source).windowSize(5).iterate(iter).trainWordVectors(false)
				.vocabCache(cache).tokenizerFactory(t).sampling(0).build();

		vec.fit();
		
		// TODO serialize the model to disk.
		
		
		INDArray interactive_regression_lens = vec.inferVector("Interactive Regression Lens");
		INDArray query = vec
				.inferVector("Abstract Data analysis often involves finding models that can explain patterns in data");
		Collection<String> strings = vec.nearestLabels(
				"Abstract Data analysis often involves finding models that can explain patterns in data", 3);
		for (String s : strings) {
			INDArray neighbor = vec.inferVector(s);
			EuclideanDistance euclideanDistance = new EuclideanDistance(query, neighbor);
			double distance = MathUtils.euclideanDistance(query.toDoubleVector(), neighbor.toDoubleVector());
			Main.LOG.info(String.format("Distance between\n%s\n%s\n%s", query, neighbor, distance));
		}

		double similarity1 = vec.similarity("DOC_9835", "DOC_12492");
		Main.LOG.info("9836/12493 ('This is my house .'/'This is my world .') similarity: " + similarity1);

		double similarity2 = vec.similarity("DOC_3720", "DOC_16392");
		Main.LOG.info("3721/16393 ('This is my way .'/'This is my work .') similarity: " + similarity2);

		double similarity3 = vec.similarity("DOC_6347", "DOC_3720");
		Main.LOG.info("6348/3721 ('This is my case .'/'This is my way .') similarity: " + similarity3);

		// likelihood in this case should be significantly lower
		double similarityX = vec.similarity("DOC_3720", "DOC_9852");
		Main.LOG.info("3721/9853 ('This is my way .'/'We now have one .') similarity: " + similarityX
				+ "(should be significantly lower)");
	}

}
