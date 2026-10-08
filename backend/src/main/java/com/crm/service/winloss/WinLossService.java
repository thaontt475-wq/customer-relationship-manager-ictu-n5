package com.crm.service.winloss;

import com.crm.dao.winloss.WinLossDAO;
import com.crm.model.winloss.Competitor;
import com.crm.model.winloss.WinLossReason;

import java.util.List;

public class WinLossService {

    private final WinLossDAO dao =
            new WinLossDAO();

    public List<WinLossReason> getReasons(
            String type,
            Boolean active
    ) throws Exception {

        if (type != null) {

            type =
                    type.trim()
                            .toUpperCase();

            if (
                    !type.equals("WON") &&
                    !type.equals("LOST")
            ) {
                throw new IllegalArgumentException(
                        "type chỉ nhận WON hoặc LOST"
                );
            }
        }

        return dao.findReasons(
                type,
                active
        );
    }

    public WinLossReason createReason(
            WinLossReason reason
    ) throws Exception {

        if (
                reason == null ||
                reason.getName() == null ||
                reason.getName().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Tên lý do là bắt buộc"
            );
        }

        if (
                reason.getType() == null ||
                reason.getType().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "type là bắt buộc"
            );
        }

        String type =
                reason.getType()
                        .trim()
                        .toUpperCase();

        if (
                !type.equals("WON") &&
                !type.equals("LOST")
        ) {
            throw new IllegalArgumentException(
                    "type chỉ nhận WON hoặc LOST"
            );
        }

        reason.setType(type);

        reason.setName(
                reason.getName().trim()
        );

        return dao.createReason(reason);
    }

    public List<Competitor> getCompetitors(
            String keyword,
            Boolean active
    ) throws Exception {

        return dao.findCompetitors(
                keyword,
                active
        );
    }

    public Competitor createCompetitor(
            Competitor competitor
    ) throws Exception {

        if (
                competitor == null ||
                competitor.getName() == null ||
                competitor.getName().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Tên đối thủ là bắt buộc"
            );
        }

        competitor.setName(
                competitor.getName().trim()
        );

        if (competitor.getNote() != null) {
            competitor.setNote(
                    competitor.getNote().trim()
            );
        }

        return dao.createCompetitor(
                competitor
        );
    }
}