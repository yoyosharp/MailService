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

    private List<String> getVariableFieldList() {
        return List.of(variableFields.split(","));
    }

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
