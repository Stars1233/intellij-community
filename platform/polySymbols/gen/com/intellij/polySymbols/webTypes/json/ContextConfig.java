
package com.intellij.polySymbols.webTypes.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Provide rules for setting a particular name for particular context kind. This allows to contribute additional Web Types for example if a particular library is present in the project.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "enable-when",
    "disable-when"
})
public class ContextConfig {

    /**
     * Specify rules for enabling web framework support. Only one framework can be enabled in a particular file. If you need your contributions to be enabled in all files, regardless of the context, do not specify the framework.
     * 
     */
    @JsonProperty("enable-when")
    @JsonPropertyDescription("Specify rules for enabling web framework support. Only one framework can be enabled in a particular file. If you need your contributions to be enabled in all files, regardless of the context, do not specify the framework.")
    private EnablementRules enableWhen;
    /**
     * Specify rules for disabling web framework support. These rules take precedence over enable-when rules. They allow to turn off framework support in case of some conflicts between frameworks priority.
     * 
     */
    @JsonProperty("disable-when")
    @JsonPropertyDescription("Specify rules for disabling web framework support. These rules take precedence over enable-when rules. They allow to turn off framework support in case of some conflicts between frameworks priority.")
    private DisablementRules disableWhen;

    /**
     * Specify rules for enabling web framework support. Only one framework can be enabled in a particular file. If you need your contributions to be enabled in all files, regardless of the context, do not specify the framework.
     * 
     */
    @JsonProperty("enable-when")
    public EnablementRules getEnableWhen() {
        return enableWhen;
    }

    /**
     * Specify rules for enabling web framework support. Only one framework can be enabled in a particular file. If you need your contributions to be enabled in all files, regardless of the context, do not specify the framework.
     * 
     */
    @JsonProperty("enable-when")
    public void setEnableWhen(EnablementRules enableWhen) {
        this.enableWhen = enableWhen;
    }

    /**
     * Specify rules for disabling web framework support. These rules take precedence over enable-when rules. They allow to turn off framework support in case of some conflicts between frameworks priority.
     * 
     */
    @JsonProperty("disable-when")
    public DisablementRules getDisableWhen() {
        return disableWhen;
    }

    /**
     * Specify rules for disabling web framework support. These rules take precedence over enable-when rules. They allow to turn off framework support in case of some conflicts between frameworks priority.
     * 
     */
    @JsonProperty("disable-when")
    public void setDisableWhen(DisablementRules disableWhen) {
        this.disableWhen = disableWhen;
    }

}
