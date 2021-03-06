/*
 *
 *  Copyright 2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package springfox.documentation.spring.web.scanners;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.BuilderDefaults;
import springfox.documentation.builders.DocumentationBuilder;
import springfox.documentation.builders.ResourceListingBuilder;
import springfox.documentation.service.ApiListing;
import springfox.documentation.service.ApiListingReference;
import springfox.documentation.service.Documentation;
import springfox.documentation.service.ResourceListing;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.service.contexts.DocumentationContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ApiDocumentationScanner {

  private ApiListingReferenceScanner apiListingReferenceScanner;
  private ApiListingScanner apiListingScanner;

  @Autowired
  public ApiDocumentationScanner(
          ApiListingReferenceScanner apiListingReferenceScanner,
          ApiListingScanner apiListingScanner) {

    this.apiListingReferenceScanner = apiListingReferenceScanner;
    this.apiListingScanner = apiListingScanner;
  }

  public Documentation scan(DocumentationContext context) {
    ApiListingReferenceScanResult result = apiListingReferenceScanner.scan(context);
    List<ApiListingReference> apiListingReferences = result.getApiListingReferences();
    ApiListingScanningContext listingContext = new ApiListingScanningContext(context, result.getResourceGroupRequestMappings());

    Map<String, ApiListing> apiListings = apiListingScanner.scan(listingContext);
    DocumentationBuilder group = new DocumentationBuilder()
            .name(context.getGroupName())
            .apiListingsByResourceGroupName(apiListings)
            .produces(context.getProduces())
            .consumes(context.getConsumes())
            .schemes(context.getProtocols())
            .basePath(context.getPathProvider().getApplicationBasePath())
            .tags(toTags(apiListings));

    Collections.sort(apiListingReferences, context.getListingReferenceOrdering());

    ResourceListing resourceListing = new ResourceListingBuilder()
            .apiVersion(context.getApiInfo().getVersion())
            .apis(apiListingReferences)
            .securitySchemes(context.getSecuritySchemes())
            .info(context.getApiInfo())
            .build();
    group.resourceListing(resourceListing);
    return group.build();
  }

  private Set<Tag> toTags(Map<String, ApiListing> apiListings) {
    return FluentIterable
            .from(BuilderDefaults.nullToEmptyMap(apiListings).entrySet())
            .transform(fromEntry())
            .toSet();
  }

  private Function<Map.Entry<String, ApiListing>, Tag> fromEntry() {
    return new Function<Map.Entry<String, ApiListing>, Tag>() {
      @Override
      public Tag apply(Map.Entry<String, ApiListing> input) {
        return new Tag(input.getKey(), input.getValue().getDescription());
      }
    };
  }

}
