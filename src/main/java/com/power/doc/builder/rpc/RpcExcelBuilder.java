/*
 * smart-doc https://github.com/shalousun/smart-doc
 *
 * Copyright (C) 2018-2020 smart-doc
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.power.doc.builder.rpc;

import static com.power.doc.constants.DocGlobalConstants.DICT_EN_TITLE;
import static com.power.doc.constants.DocGlobalConstants.ERROR_CODE_LIST_CN_TITLE;
import static com.power.doc.constants.DocGlobalConstants.ERROR_CODE_LIST_EN_TITLE;
import static com.power.doc.constants.DocGlobalConstants.ERROR_CODE_LIST_MD_TPL;
import static com.power.doc.constants.DocGlobalConstants.FILE_SEPARATOR;
import static com.power.doc.constants.DocGlobalConstants.HTML_API_DOC_TPL;
import static com.power.doc.constants.DocGlobalConstants.INDEX_CSS_TPL;
import static com.power.doc.constants.DocGlobalConstants.MARKDOWN_CSS_TPL;
import static com.power.doc.constants.DocGlobalConstants.RPC_DEPENDENCY_MD_TPL;
import static com.power.doc.constants.DocGlobalConstants.RPC_EXCEL_TPL;
import static com.power.doc.constants.DocGlobalConstants.RPC_INDEX_TPL;
import static com.power.doc.constants.DocGlobalConstants.RPC_SINGLE_EXCEL_TPL;

import com.power.common.util.CollectionUtil;
import com.power.common.util.DateTimeUtil;
import com.power.common.util.FileUtil;
import com.power.doc.builder.ProjectDocConfigBuilder;
import com.power.doc.constants.DocLanguage;
import com.power.doc.constants.TemplateVariable;
import com.power.doc.model.ApiConfig;
import com.power.doc.model.ApiErrorCode;
import com.power.doc.model.rpc.RpcApiDependency;
import com.power.doc.model.rpc.RpcApiDoc;
import com.power.doc.template.RpcDocBuildTemplate;
import com.power.doc.utils.BeetlTemplateUtil;
import com.power.doc.utils.MarkDownUtil;
import com.thoughtworks.qdox.JavaProjectBuilder;
import java.util.List;
import java.util.Objects;
import org.beetl.core.Template;

/**
 * @author yu 2020/5/17.
 */
public class RpcExcelBuilder {

    private static long now = System.currentTimeMillis();

    private static String INDEX_HTML = "rpc.xls";


    /**
     * build controller api
     *
     * @param config config
     */
    public static void buildApiDoc(ApiConfig config) {
        JavaProjectBuilder javaProjectBuilder = new JavaProjectBuilder();
        buildApiDoc(config, javaProjectBuilder);
    }

    /**
     * Only for smart-doc maven plugin and gradle plugin.
     *
     * @param config             ApiConfig
     * @param javaProjectBuilder ProjectDocConfigBuilder
     */
    public static void buildApiDoc(ApiConfig config, JavaProjectBuilder javaProjectBuilder) {
        config.setShowJavaType(true);
        RpcDocBuilderTemplate builderTemplate = new RpcDocBuilderTemplate();
        builderTemplate.checkAndInit(config);
        builderTemplate.checkAndInit(config);
        ProjectDocConfigBuilder configBuilder = new ProjectDocConfigBuilder(config, javaProjectBuilder);
        RpcDocBuildTemplate docBuildTemplate = new RpcDocBuildTemplate();
        List<RpcApiDoc> apiDocList = docBuildTemplate.getApiData(configBuilder);
        if (config.isAllInOne()) {
            builderTemplate.buildAllInOne(apiDocList, config, javaProjectBuilder, RPC_EXCEL_TPL, INDEX_HTML);
        } else {
            buildDoc(apiDocList, config.getOutPath(),config);
        }
    }

    private static void copyCss(String outPath) {
        Template indexCssTemplate = BeetlTemplateUtil.getByName(INDEX_CSS_TPL);
        Template mdCssTemplate = BeetlTemplateUtil.getByName(MARKDOWN_CSS_TPL);
        FileUtil.nioWriteFile(indexCssTemplate.render(), outPath + FILE_SEPARATOR + INDEX_CSS_TPL);
        FileUtil.nioWriteFile(mdCssTemplate.render(), outPath + FILE_SEPARATOR + MARKDOWN_CSS_TPL);
    }

    /**
     * build api.html
     *
     * @param apiDocList list of api doc
     * @param config     ApiConfig
     */
    private static void buildIndex(List<RpcApiDoc> apiDocList, ApiConfig config) {
        FileUtil.mkdirs(config.getOutPath());
        Template indexTemplate = BeetlTemplateUtil.getByName(RPC_INDEX_TPL);
        if (CollectionUtil.isEmpty(apiDocList)) {
            return;
        }
        RpcApiDoc doc = apiDocList.get(0);
        String homePage = doc.getAlias();
        indexTemplate.binding(TemplateVariable.HOME_PAGE.getVariable(), homePage);
        indexTemplate.binding(TemplateVariable.API_DOC_LIST.getVariable(), apiDocList);
        indexTemplate.binding(TemplateVariable.VERSION.getVariable(), now);
        indexTemplate.binding(TemplateVariable.ERROR_CODE_LIST.getVariable(), config.getErrorCodes());
        indexTemplate.binding(TemplateVariable.DICT_LIST.getVariable(), config.getDataDictionaries());
        if (CollectionUtil.isEmpty(config.getErrorCodes())) {
            indexTemplate.binding(TemplateVariable.DICT_ORDER.getVariable(), apiDocList.size() + 2);
        } else {
            indexTemplate.binding(TemplateVariable.DICT_ORDER.getVariable(), apiDocList.size() + 3);
        }
        if (null != config.getLanguage()) {
            if (DocLanguage.CHINESE.code.equals(config.getLanguage().getCode())) {
                indexTemplate.binding(TemplateVariable.ERROR_LIST_TITLE.getVariable(), ERROR_CODE_LIST_CN_TITLE);
            } else {
                indexTemplate.binding(TemplateVariable.ERROR_LIST_TITLE.getVariable(), ERROR_CODE_LIST_EN_TITLE);
            }
        } else {
            indexTemplate.binding(TemplateVariable.ERROR_LIST_TITLE.getVariable(), ERROR_CODE_LIST_CN_TITLE);
        }
        FileUtil.nioWriteFile(indexTemplate.render(), config.getOutPath() + FILE_SEPARATOR + "rpc-api.html");
    }

    /**
     * build ever controller api
     *
     * @param apiDocList list of api doc
     * @param outPath    output path
     */
    private static void buildDoc(List<RpcApiDoc> apiDocList, String outPath,ApiConfig config) {
        FileUtil.mkdirs(outPath);
        for (RpcApiDoc rpcDoc : apiDocList) {
            Template apiTemplate = BeetlTemplateUtil.getByName(RPC_SINGLE_EXCEL_TPL);
            apiTemplate.binding(TemplateVariable.API.getVariable(), rpcDoc);
            apiTemplate.binding(TemplateVariable.APP_NAME.getVariable(), config.getAppName());
            FileUtil.nioWriteFile(apiTemplate.render(), outPath + FILE_SEPARATOR + rpcDoc.getAlias() + ".xls");
        }
    }

    /**
     * build error_code html
     *
     * @param errorCodeList list of error code
     * @param outPath
     */
    private static void buildErrorCodeDoc(List<ApiErrorCode> errorCodeList, String outPath) {
        if (CollectionUtil.isNotEmpty(errorCodeList)) {
            Template error = BeetlTemplateUtil.getByName(ERROR_CODE_LIST_MD_TPL);
            error.binding(TemplateVariable.LIST.getVariable(), errorCodeList);
            String errorHtml = MarkDownUtil.toHtml(error.render());
            Template errorCodeDoc = BeetlTemplateUtil.getByName(HTML_API_DOC_TPL);
            errorCodeDoc.binding(TemplateVariable.VERSION.getVariable(), now);
            errorCodeDoc.binding(TemplateVariable.HTML.getVariable(), errorHtml);
            errorCodeDoc.binding(TemplateVariable.TITLE.getVariable(), ERROR_CODE_LIST_EN_TITLE);
            errorCodeDoc.binding(TemplateVariable.CREATE_TIME.getVariable(), DateTimeUtil.long2Str(now, DateTimeUtil.DATE_FORMAT_SECOND));
            FileUtil.nioWriteFile(errorCodeDoc.render(), outPath + FILE_SEPARATOR + "error_code.html");
        }
    }

    /**
     * build dictionary
     *
     * @param config dictionary list
     */
    private static void buildDependency(ApiConfig config) {
        List<RpcApiDependency> apiDependencies = config.getRpcApiDependencies();
        if (CollectionUtil.isNotEmpty(config.getRpcApiDependencies())) {
            String rpcConfig = config.getRpcConsumerConfig();
            String rpcConfigConfigContent = null;
            if (Objects.nonNull(rpcConfig)) {
                rpcConfigConfigContent = FileUtil.getFileContent(rpcConfig);
            }
            Template template = BeetlTemplateUtil.getByName(RPC_DEPENDENCY_MD_TPL);
            template.binding(TemplateVariable.RPC_CONSUMER_CONFIG.getVariable(), rpcConfigConfigContent);
            template.binding(TemplateVariable.DEPENDENCY_LIST.getVariable(), apiDependencies);
            String dictHtml = MarkDownUtil.toHtml(template.render());
            Template dictTpl = BeetlTemplateUtil.getByName(HTML_API_DOC_TPL);
            dictTpl.binding(TemplateVariable.VERSION.getVariable(), now);
            dictTpl.binding(TemplateVariable.TITLE.getVariable(), DICT_EN_TITLE);
            dictTpl.binding(TemplateVariable.HTML.getVariable(), dictHtml);
            dictTpl.binding(TemplateVariable.CREATE_TIME.getVariable(), DateTimeUtil.long2Str(now, DateTimeUtil.DATE_FORMAT_SECOND));
            FileUtil.nioWriteFile(dictTpl.render(), config.getOutPath() + FILE_SEPARATOR + "dependency.html");
        }
    }
}
