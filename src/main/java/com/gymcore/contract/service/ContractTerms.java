package com.gymcore.contract.service;

import org.springframework.stereotype.Component;

@Component
public class ContractTerms {
    public record Terms(String version, String text, String acceptanceMode) {}
    public Terms current() {
        return new Terms("2026-10-v1", "Plano mensal com primeira mensalidade integral, sem pró-rata. "
            + "Vencimentos seguintes no dia de início, ajustados ao último dia do mês. "
            + "Cancelamento com data final inclusiva preserva dívidas e não gera multa nem reembolso. "
            + "Aceite presencial registrado pela recepção, sem verificação digital de identidade.", "IN_PERSON_RECORDED");
    }
}
