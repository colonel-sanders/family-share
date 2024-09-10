package com.github.colonelsanders.familyshare.controllers;

import com.github.colonelsanders.familyshare.entities.Category;
import com.github.colonelsanders.familyshare.entities.CategoryRepository;
import com.github.colonelsanders.familyshare.entities.Document;
import com.github.colonelsanders.familyshare.entities.DocumentRepository;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.io.IOException;
import java.io.Writer;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/secure/")
public class SecuredContentController {
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private DocumentRepository documentRepository;

  @ModelAttribute
  public void mustacheMarkdownRendererFactory(Model model) {
    model.addAttribute(
        "renderMarkdown",
        new Mustache.Lambda() {
          @Override
          public void execute(Template.Fragment fragment, Writer writer) throws IOException {
            var markdownContent = fragment.execute();
            var markdownNode = Parser.builder().build().parse(markdownContent);
            var renderer = HtmlRenderer.builder().escapeHtml(true).build();
            writer.append(renderer.render(markdownNode));
          }
        });
  }

  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("categories", categoryRepository.findAll());
    return "secure";
  }

  @GetMapping("categories/{categoryId}/documents/new")
  public String newDocumentGet(@PathVariable Long categoryId, Model model) {
    var category = categoryRepository.findById(categoryId);
    model.addAttribute("category", category.orElseThrow());
    return "_document_create";
  }

  @PostMapping("categories/{categoryId}/documents")
  public String newDocumentPost(
      @PathVariable Long categoryId,
      @RequestParam String name,
      @RequestParam String content,
      Model model) {
    var category = categoryRepository.findById(categoryId);
    var document =
        saveWithConstraintCheck(
            new Document(category.orElseThrow(), null, name, content), documentRepository);
    model.addAttribute("category", category.orElseThrow());
    model.addAttribute("documents", new Document[] {document});
    return "_documents_view";
  }

  @GetMapping("categories/{categoryId}/documents")
  public String categoryDocumentsGet(@PathVariable Long categoryId, Model model) {
    var category = categoryRepository.findById(categoryId);
    model.addAttribute("category", category.orElseThrow());
    model.addAttribute("documents", documentRepository.findAllByCategory(category.orElseThrow()));
    return "_documents_view";
  }

  @GetMapping("categories/new")
  public String newCategoryGet() {
    return "_category_create";
  }

  @PostMapping("categories")
  public String newCategoryPost(@RequestParam String name, Model model) {
    var cat = saveWithConstraintCheck(new Category(null, name), categoryRepository);
    model.addAttribute("categories", new Category[] {cat});
    return "_categories_view";
  }

  private static <TEntity> TEntity saveWithConstraintCheck(
      TEntity entity, CrudRepository<TEntity, Long> repository) {
    try {
      return repository.save(entity);
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate name", e);
    }
  }
}
