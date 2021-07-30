package qio.support;

import qio.Qio;
import qio.processor.ElementProcessor;
import qio.storage.ElementStorage;
import qio.storage.PropertyStorage;
import qio.jdbc.BasicDataSource;
import qio.model.Element;
import qio.processor.AnnotationProcessor;
import qio.processor.ConfigurationProcessor;
import qio.processor.EndpointProcessor;
import qio.model.web.HttpMappings;

public class Initializer {

    public Initializer(Builder builder){}

    public static class Builder {

        Qio qio;
        String[] resources;
        ElementStorage elementStorage;
        ElementProcessor elementProcessor;
        PropertyStorage propertyStorage;

        EndpointProcessor endpointProcessor;
        AnnotationProcessor annotationProcessor;
        ConfigurationProcessor configurationProcessor;

        public Builder withQio(Qio qio){
            this.qio = qio;
            return this;
        }
        public Builder withResources(String[] resources){
            this.resources = resources;
            return this;
        }
        public Builder withElementStorage(ElementStorage elementStorage){
            this.elementStorage = elementStorage;
            return this;
        }
        public Builder withElementProcessor(ElementProcessor elementProcessor){
            this.elementProcessor = elementProcessor;
            return this;
        }
        public Builder withPropertyStorage(PropertyStorage propertyStorage){
            this.propertyStorage = propertyStorage;
            return this;
        }

        private void setQioElement(){
            Element qioElement = new Element();
            qioElement.setElement(qio);
            elementStorage.getElements().put(Qio.QIO, qioElement);
            Qio.set(elementStorage.getElements());
            Qio.servletContext.setAttribute(Qio.QIO, qioElement);
        }

        private void setHttpResources(){
            Qio.servletContext.setAttribute(Qio.HTTP_RESOURCES, resources);
        }
        
        private void checkInitDevDb() throws Exception{
            if (Qio.devMode){
                DbMediator mediator = new DbMediator(Qio.servletContext);
                Element element = new Element();
                element.setElement(mediator);
                elementStorage.getElements().put(Qio.DBMEDIATOR, element);
                mediator.createDb();
            }
        }

        private void runConfigProcessor() throws Exception {
            if(elementProcessor.getConfigs() != null &&
                    elementProcessor.getConfigs().size() > 0){
                configurationProcessor = new ConfigurationProcessor(elementStorage, elementProcessor, propertyStorage);
                configurationProcessor.run();
            }
        }

        private void runAnnotationProcessor() throws Exception {
            annotationProcessor = new AnnotationProcessor(elementStorage, elementProcessor, propertyStorage);
            annotationProcessor.run();
        }

        private void runEndpointProcessor() throws Exception {
            System.out.println(Qio.Assistant.SIGNATURE + " processing endpoints");
            endpointProcessor = new EndpointProcessor(elementStorage, elementProcessor);
            endpointProcessor.run();
        }

        private void validateDatasource() throws Exception {
            if(Qio.dataEnabled != null &&
                    Qio.dataEnabled) {
                System.out.println(Qio.Assistant.SIGNATURE + " validating datasource");
                Element element = elementStorage.getElements().get(Qio.DATASOURCE);
                if(element == null){
                    Qio.Injector.badge();
                    throw new Exception("No data source configured... \nmake sure the method name in your config for your data source is named 'dataSource'\n\n\n\n\n");
                }
                BasicDataSource basicDataSource = (BasicDataSource) element.getElement();
                qio.setDataSource(basicDataSource);
            }
        }

        protected void setHttpMappings(){
            HttpMappings httpMappings = endpointProcessor.getMappings();
            Qio.servletContext.setAttribute(Qio.HTTP_MAPPINGS, httpMappings);
        }

        private void sayReady(){
            System.out.println(Qio.Assistant.SIGNATURE + " project ready \u2713");
            System.out.println(Qio.Assistant.SIGNATURE + " Go to \033[1;33mhttp://localhost:8080" + Qio.servletContext.getContextPath() + "\033[0m port may differ\n\n\n\n\n");
        }

        private void validateResources(){
            if(resources == null) resources = new String[]{};
        }

        private void setQioAttributes(){
            setQioElement();
            setHttpResources();
            validateResources();
        }

        private void setQioDbAttributes() throws Exception {
            validateDatasource();
            checkInitDevDb();
        }

        public Builder initialize() throws Exception {

            setQioAttributes();
            runConfigProcessor();
            runAnnotationProcessor();
            runEndpointProcessor();
            setQioDbAttributes();
            setHttpMappings();

            sayReady();
            return this;
        }

        public Initializer build() {
            return new Initializer(this);
        }
    }

}
