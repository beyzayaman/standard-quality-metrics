/**
 * 
 */
package eu.diachron.qualitymetrics.accessibility.availability;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.sparql.core.Quad;
import de.unibonn.iai.eis.diachron.semantics.DQM;
import de.unibonn.iai.eis.diachron.semantics.DQMPROB;
import de.unibonn.iai.eis.diachron.semantics.knownvocabs.GEOSPARQL;
import de.unibonn.iai.eis.diachron.technques.probabilistic.ReservoirSampler;
import de.unibonn.iai.eis.luzzu.datatypes.ProblemList;
import de.unibonn.iai.eis.luzzu.semantics.vocabularies.QPRO;
import eu.diachron.qualitymetrics.utilities.AbstractQualityMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 /**
 * @author Beyza Yaman
 *
 * This metric calculates the number of valid number of the links to Spatial Things from popular repositories which increases the discoverability of the dataset.
 * Since most of the users query popular knowledge bases to access knowledge, creating links to these knowledge bases increases the access to the published datasets as well.
 *
 *
 * Based on:
 *
 * Best Practise: Use appropriate relation types to link Spatial Things where source and target of the hyperlink are Spatial Things
 *
 *
 */
public class LinkSpatialThingfromKGCheck extends AbstractQualityMetric {

	private final Resource METRIC_URI = DQM.LinkToSpatialThingfromKGMetric;

	private static Logger logger = LoggerFactory.getLogger(LinkSpatialThingfromKGCheck.class);

//	private static DB mapDb = MapDbFactory.getMapDBAsyncTempFile();
//	protected Set<SerialisableModel> problemList =  MapDbFactory.createHashSet(mapDb, UUID.randomUUID().toString());

	// Sampling of problems - testing for LOD Evaluation
	ReservoirSampler<ProblemReport> problemSampler = new ReservoirSampler<ProblemReport>(50000, false);

	private int instanceCount = 0;
	private int buildingCount = 0;
	private int formCount = 0;
	private int propertyCount = 0;
	private int geometryCount = 0;
	private String resourceURI = "";
	private boolean missingSpatialLinkProperty =true;
	private HashMap<String, String> resourceMap=new HashMap();

	/**
	 * This method computes identified a given quad is a misuse owl data type
	 * property or object property.
	 *
	 * @param quad - to be identified
	 *
	 **/
	
	public void compute(Quad quad) {
		logger.debug("Computing : {} ", quad.asTriple().toString());

		Node subject = quad.getSubject();
		Node predicate = quad.getPredicate();
		Node object = quad.getObject();

		//These are the properties used for the spatial links for each dataset
		String lgdURI= "http://linkedgeodata.org/ontology/wikidata";
		String osiURI= "http://open.vocab.org/terms/similarTo";

		String tripleSubjectURI=subject.toString();

		if(resourceURI.equals("")) resourceURI=tripleSubjectURI;
		if(resourceURI.equals(tripleSubjectURI)){
			checkStandard(object,predicate,subject, osiURI);
		}
		else {
			resourceURI = tripleSubjectURI;
			checkStandard(object,predicate,subject, osiURI);
		}
	}

	private void createProblemModel(Node resource, Node property, Resource type){
		ProblemReport pr = new ProblemReport(resource, property, type);
		Boolean isAdded = this.problemSampler.add(pr);
		if (!isAdded) pr = null;
	}
	private void checkStandard(Node object, Node predicate, Node subject, String URI) {

		//count number of instances in the dataset with their type to discover the amount according to standards
		if (object.isURI()) {
			if ((object.getURI().equals("http://www.opengis.net/ont/geosparql#Feature"))) this.instanceCount++;
			if ((object.getURI().equals("http://ontologies.geohive.ie/osi#Building"))) this.buildingCount++;
			if ((object.getURI().equals("http://ontologies.geohive.ie/geoff#Form16"))) this.formCount++;

			if ((object.getURI().equals(GEOSPARQL.Geometry.getURI()))) this.geometryCount++; //Geosparql Ontology
			if ((object.getURI().equals("http://geovocab.org/geometry#Geometry")))
				this.geometryCount++; //Neogeo Ontology
			if ((object.getURI().equals("http://geo.linkedopendata.gr/coastline_gr/ontology#GeometryPart")))
				this.geometryCount++; //Greek Ontology coastline
			if ((object.getURI().equals("http://geo.linkedopendata.gr/corine/ontology#Area")))
				this.geometryCount++; //Greek Ontology water bodies
			if ((object.getURI().equals("http://data.ordnancesurvey.co.uk/ontology/geometry/AbstractGeometry")))
				this.geometryCount++; //OS UK Ontology parishes
		}

		if ((predicate.getURI().equals(URI))) {
			propertyCount++;
			resourceMap.put(subject.toString(), object.toString());
			missingSpatialLinkProperty = false;
		}
	}

	/**
	 * This method computes metric value for the object of this class
	 *
	 * @return (total misuse properties) / (total properties)
	 */

	public double metricValue() {
		/**System.out.println(instanceCount);
		System.out.println(buildingCount);
		System.out.println(formCount);
		System.out.println(GeometryCount);
		System.out.println(propertyCount);*/
		double metricValue = 0.0;
		if(missingSpatialLinkProperty==false) {
			for(Map.Entry<String, String> entry: resourceMap.entrySet()){
				if(entry.getKey().isEmpty()){
					createProblemQuad(entry.getValue().toString(),DQMPROB.MissingSpatialThingLinkFromKG);
				}}
			metricValue = ((double) this.propertyCount / this.geometryCount);
		}

		statsLogger.info("Dataset: {}; Number of Missing Spatial Thing Links from KG: {}, Number of instances : {}, Metric Value: {}",this.getDatasetURI(),this.propertyCount, this.geometryCount, metricValue);
		return metricValue;
	}

	private void createProblemQuad(String resource, Resource problem){
		Quad q = new Quad(null, ModelFactory.createDefaultModel().createResource(resource).asNode(), QPRO.exceptionDescription.asNode(), problem.asNode());
		//this._problemList.add(q);
	}
	/**
	 * Returns Metric URI
	 *
	 * @return metric URI
	 */
	public Resource getMetricURI() {
		return this.METRIC_URI;
	}

	/**
	 * Returns list of problematic Quads
	 *
	 * @return list of problematic quads
	 */

//	public ProblemList<?> getQualityProblems() {
//		ProblemList<SerialisableModel> tmpProblemList = null;
//		try {
//			if(this.problemList != null && this.problemList.size() > 0) {
//				tmpProblemList = new ProblemList<SerialisableModel>(new ArrayList<SerialisableModel>(this.problemList));
//			} else {
//				tmpProblemList = new ProblemList<SerialisableModel>();
//			}		} catch (ProblemListInitialisationException problemListInitialisationException) {
//			logger.error(problemListInitialisationException.getMessage());
//		}
//		return tmpProblemList;
//	}


	public ProblemList<?> getQualityProblems() {
		ProblemList<Model> tmpProblemList = new ProblemList<Model>();
		if(this.problemSampler != null && this.problemSampler.size() > 0) {
			for(ProblemReport pr : this.problemSampler.getItems()){
				tmpProblemList.getProblemList().add(pr.createProblemModel());
			}
		} else {
			tmpProblemList = new ProblemList<Model>();
		}
		return tmpProblemList;
	}

	@Override
	public boolean isEstimate() {
		return false;
	}

	@Override
	public Resource getAgentURI() {
		return DQM.LuzzuProvenanceAgent;
	}

	private class ProblemReport{

		private Resource type;
		private Node resource;
		private Node property;

		ProblemReport(Node resource, Node property, Resource type){
			this.resource = resource;
			this.property = property;
			this.type = type;
		}

		Model createProblemModel(){
			Model m = ModelFactory.createDefaultModel();

			Resource subject = m.createResource(resource.toString());
			m.add(new StatementImpl(subject, QPRO.exceptionDescription, type));

			if (type.equals(DQMPROB.MisusedDatatypeProperty))
				m.add(new StatementImpl(subject, DQMPROB.hasMisusedDatatypeProperty, m.asRDFNode(property)));
			else
				m.add(new StatementImpl(subject, DQMPROB.hasMisusedObjectProperty, m.asRDFNode(property)));


			return m;
		}
	}
}
