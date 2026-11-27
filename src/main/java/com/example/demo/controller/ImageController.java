package com.example.demo.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.dto.ImageEditForm;
import com.example.demo.dto.ImageUploadForm;
import com.example.demo.dto.ImageView;
import com.example.demo.dto.PageResult;
import com.example.demo.dto.SessionUser;
import com.example.demo.model.Tag;
import com.example.demo.service.ImageService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PostMapping;

@Controller 
@RequestMapping("/user")
@RequiredArgsConstructor 
public class ImageController {
    private final ImageService imageService;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String keyword, @RequestParam(required = false) List<Long> tagIds, @RequestParam(defaultValue = "1") int page, Model model) {
        fillGalleryModel(keyword, tagIds, null, page, model);
        return "test/user-dashboard";
    }
    
    @GetMapping("/gallery")
    public String userGallery(@RequestParam(required = false) String keyword, @RequestParam(required = false) List<Long> tagIds, @RequestParam(defaultValue = "1") int page, HttpSession session, Model model) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null) return "redirect:/login";

        fillGalleryModel(keyword, tagIds, sessionUser.id(), page, model);
        return "test/user-gallery";
    }
    
    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        ImageUploadForm uploadForm = new ImageUploadForm();
        List<Tag> tags = imageService.getAllTags();

        model.addAttribute("uploadForm", uploadForm);
        model.addAttribute("tags", tags);
        return "test/upload-image";
    }
    
    @PostMapping("/upload")
    public String uploadImage(@Valid @ModelAttribute("uploadForm") ImageUploadForm form, BindingResult res, HttpSession session, RedirectAttributes attributes, Model model) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null) return "redirect:/login";

        if (form.getFile() == null || form.getFile().isEmpty()) {
            res.rejectValue("file", "file.empty", "Foto harus diupload");
        }

        if (res.hasErrors()) {
            model.addAttribute("tags", imageService.getAllTags());
            return "test/upload-image";
        }

        try {
            imageService.uploadImage(sessionUser, form);
            attributes.addFlashAttribute("success", "Foto berhasil diunggah");
            return "redirect:/user/gallery";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("tags", imageService.getAllTags());
            return "test/upload-image";
        }
    }
    
    @GetMapping("/gallery/{id}/edit")
    public String showEditForm(@PathVariable Long id, HttpSession session, RedirectAttributes attributes, Model model) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null) return "redirect:/login";

        try {
            ImageView existing = imageService.getImgForEdit(id, sessionUser);
            ImageEditForm form = new ImageEditForm();

            form.setDescription(existing.getDescription());
            form.setTagIds(existing.getTags().stream().map(Tag::getId).toList());

            model.addAttribute("editForm", form);
            model.addAttribute("image", existing);
            model.addAttribute("tags", imageService.getAllTags());

            return "test/edit-image";
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/gallery";
        }
    }

    @PostMapping("/gallery/{id}/edit")
    public String updateImage(@PathVariable Long id, @Valid @ModelAttribute("editForm") ImageEditForm form, BindingResult res, HttpSession session, RedirectAttributes attributes, Model model) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null) return "redirect:/login";
        
        ImageView existing;
        try {
            existing = imageService.getImgForEdit(id, sessionUser);
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/gallery";
        }

        if (res.hasErrors()) {
            model.addAttribute("image", existing);
            model.addAttribute("tags", imageService.getAllTags());
            return "test/edit-image";
        }

        try {
            imageService.updateImage(id, sessionUser, form);
            attributes.addFlashAttribute("success", "Perubahan berhasil disimpan");
            return "redirect:/user/gallery";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("image", existing);
            model.addAttribute("tags", imageService.getAllTags());
            return "test/edit-image";
        }
    }
    
    @PostMapping("/gallery/{id}/delete")
    public String deleteImage(@PathVariable Long id, HttpSession session, RedirectAttributes attributes) {
        SessionUser sessionUser = getSessionUser(session);
        if (sessionUser == null) return "redirect:/login";

        try {
            imageService.deleteImage(id, sessionUser);
            attributes.addFlashAttribute("success", "Foto berhasil dihapus");
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/user/gallery";
    }
    
    private SessionUser getSessionUser(HttpSession session) {
        SessionUser sessionUser = (session != null && session.getAttribute("user") instanceof SessionUser) ? (SessionUser) session.getAttribute("user") : null;
        return sessionUser;
    }

    private void fillGalleryModel(String keyword, List<Long> tagIds, Long ownerUserId, int page, Model model) {
        PageResult<ImageView> gallery = imageService.getGallery(keyword, tagIds, ownerUserId, page);
        List<Tag> tags = imageService.getAllTags();
        keyword = keyword != null ? keyword : "";
        tagIds = tagIds != null ? tagIds : List.of();
        
        model.addAttribute("gallery", gallery);
        model.addAttribute("tags", tags);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedTagIds", tagIds);
    }
}
