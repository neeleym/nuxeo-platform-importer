/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *    Mariana Cedica
 */
package org.nuxeo.ecm.platform.importer.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunnerConfiguration;
import org.nuxeo.ecm.platform.importer.executor.AbstractImporterExecutor;
import org.nuxeo.ecm.platform.importer.executor.DefaultImporterExecutor;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.filter.EventServiceConfiguratorFilter;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public class DefaultImporterServiceImpl implements DefaultImporterService {

    private static Log log = LogFactory.getLog(DefaultImporterServiceImpl.class);

    private Class<? extends ImporterDocumentModelFactory> docModelFactoryClass;

    private Class<? extends SourceNode> sourceNodeClass;

    private ImporterDocumentModelFactory documentModelFactory;

    private String folderishDocType;

    private String leafDocType;

    private ImporterLogger importerLogger;

    private int transactionTimeout = 0;
    
    private String repositoryName;

    @Override
    public void importDocuments(String destinationPath, String sourcePath, boolean skipRootContainerCreation,
            int batchSize, int noImportingThreads) {
        SourceNode sourceNode = createNewSourceNodeInstanceForSourcePath(sourcePath);
        if (sourceNode == null) {
            log.error("Need to set a sourceNode to be used by this importer");
            return;
        }
        if (getDocumentModelFactory() == null) {
            log.error("Need to set a documentModelFactory to be used by this importer");
        }

        DefaultImporterExecutor executor = new DefaultImporterExecutor(repositoryName);
        executor.setFactory(getDocumentModelFactory());
        executor.setTransactionTimeout(transactionTimeout);
        executor.run(sourceNode, destinationPath, skipRootContainerCreation, batchSize, noImportingThreads, true);
    }

    @Override
    public String importDocuments(AbstractImporterExecutor executor, String destinationPath, String sourcePath,
            boolean skipRootContainerCreation, int batchSize, int noImportingThreads, boolean interactive)
            {

        SourceNode sourceNode = createNewSourceNodeInstanceForSourcePath(sourcePath);
        if (sourceNode == null) {
            log.error("Need to set a sourceNode to be used by this importer");
            return "Can not import";
        }
        if (getDocumentModelFactory() == null) {
            log.error("Need to set a documentModelFactory to be used by this importer");
        }

        ImporterRunnerConfiguration configuration = new ImporterRunnerConfiguration.Builder(sourceNode,
                destinationPath, executor.getLogger()).skipRootContainerCreation(skipRootContainerCreation).batchSize(
                batchSize).nbThreads(noImportingThreads).repository(repositoryName).build();
        GenericMultiThreadedImporter runner = new GenericMultiThreadedImporter(configuration);
        runner.setTransactionTimeout(transactionTimeout);
        ImporterFilter filter = new EventServiceConfiguratorFilter(false, false, false, true);
        runner.addFilter(filter);
        runner.setFactory(getDocumentModelFactory());
        return executor.run(runner, interactive);
    }

    @Override
    public String importDocuments(AbstractImporterExecutor executor, String leafType, String folderishType,
            String destinationPath, String sourcePath, boolean skipRootContainerCreation, int batchSize,
            int noImportingThreads, boolean interactive) {
        ImporterDocumentModelFactory docModelFactory = getDocumentModelFactory();
        if (docModelFactory instanceof DefaultDocumentModelFactory) {
            DefaultDocumentModelFactory defaultDocModelFactory = (DefaultDocumentModelFactory) docModelFactory;
            defaultDocModelFactory.setLeafType(leafType == null ? getLeafDocType() : leafType);
            defaultDocModelFactory.setFolderishType(folderishType == null ? getFolderishDocType() : folderishType);
        }
        setDocumentModelFactory(docModelFactory);
        executor.setTransactionTimeout(transactionTimeout);
        String res = importDocuments(executor, destinationPath, sourcePath, skipRootContainerCreation, batchSize,
                noImportingThreads, interactive);
        setDocumentModelFactory(null);
        return res;

    }

    @Override
    public void setDocModelFactoryClass(Class<? extends ImporterDocumentModelFactory> docModelFactoryClass) {
        this.docModelFactoryClass = docModelFactoryClass;
    }

    @Override
    public void setSourceNodeClass(Class<? extends SourceNode> sourceNodeClass) {
        this.sourceNodeClass = sourceNodeClass;
    }

    protected SourceNode createNewSourceNodeInstanceForSourcePath(String sourcePath) {
        SourceNode sourceNode = null;
        if (sourceNodeClass != null && FileSourceNode.class.isAssignableFrom(sourceNodeClass)) {
            try {
                sourceNode = sourceNodeClass.getConstructor(String.class).newInstance(sourcePath);
            } catch (ReflectiveOperationException e) {
                log.error(e, e);
            }
        }
        return sourceNode;
    }

    protected ImporterDocumentModelFactory getDocumentModelFactory() {
        if (documentModelFactory == null) {
            if (docModelFactoryClass != null
                    && DefaultDocumentModelFactory.class.isAssignableFrom(docModelFactoryClass)) {
                try {
                    setDocumentModelFactory(docModelFactoryClass.getConstructor(String.class, String.class).newInstance(
                            getFolderishDocType(), getLeafDocType()));
                } catch (ReflectiveOperationException e) {
                    log.error(e, e);
                }
            }
        }
        return documentModelFactory;
    }

    protected void setDocumentModelFactory(ImporterDocumentModelFactory documentModelFactory) {
        this.documentModelFactory = documentModelFactory;
    }

    public String getFolderishDocType() {
        return folderishDocType;
    }

    @Override
    public void setFolderishDocType(String folderishDocType) {
        this.folderishDocType = folderishDocType;
    }

    public String getLeafDocType() {
        return leafDocType;
    }

    @Override
    public void setLeafDocType(String fileDocType) {
        leafDocType = fileDocType;
    }

    public ImporterLogger getImporterLogger() {
        return importerLogger;
    }

    @Override
    public void setImporterLogger(ImporterLogger importerLogger) {
        this.importerLogger = importerLogger;
    }

    /*
     * @since 5.9.4
     */
    @Override
    public void setTransactionTimeout(int transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
    }

    /*
     * @since 5.7.3
     */
    @Override
    public Class<? extends SourceNode> getSourceNodeClass() {
        return sourceNodeClass;
    }

    /*
     * @since 5.7.3
     */
    @Override
    public Class<? extends ImporterDocumentModelFactory> getDocModelFactoryClass() {
        return docModelFactoryClass;
    }
    
    /**
     *@since 7.1 
     * @param repositoryName
     */
    @Override
    public void setRepository(String repositoryName) {
        this.repositoryName=repositoryName;
    }
}
