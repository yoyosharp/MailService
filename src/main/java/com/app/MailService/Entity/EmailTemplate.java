package com.app.MailService.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "email_templates")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "template")
    private String template;

    @Column(name = "variable_fields")
    private String variableFields;

    @Column(name = "is_active")
    private boolean isActive;

    /**
     * Retrieves the list of variable fields from the entity.
     * variableFields is a comma-separated list of field names
     *
     * @return list of variable fields
     */
    public List<String> getVariableFieldList() {
        return List.of(variableFields.split(","));
    }

    /**
     * Fills the template with the provided variables.
     *
     * @param variables a map of key-value pairs representing the variables to replace in the template
     * @return the filled template with replaced variables
     */
    public String fillTemplate(Map<String, String> variables) {
        List<String> variableFields = getVariableFieldList();
        String filledTemplate = template;
        for (String variableField : variableFields) {
            filledTemplate = filledTemplate.replace("{{" + variableField.toUpperCase() + "}}", variables.get(variableField));
        }
        return filledTemplate;
    }

    public boolean isActive() {
        return isActive;
    }
}
