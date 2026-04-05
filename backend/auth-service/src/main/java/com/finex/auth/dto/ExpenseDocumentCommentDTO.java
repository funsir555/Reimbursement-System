package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseDocumentCommentDTO {

    private String comment;

    private List<String> attachmentFileNames = new ArrayList<>();
}
