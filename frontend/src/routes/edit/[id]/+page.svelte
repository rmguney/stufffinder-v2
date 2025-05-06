<script>
    import * as Card from "$lib/components/ui/card/index.js";
    import { Button } from "$lib/components/ui/button";
    import Query from '$lib/components/query.svelte';
    import { threadStore, updateThread } from '../../../threadStore';
    import { goto } from '$app/navigation';
    import { activeUser } from '../../../userStore';
    import { Textarea } from "$lib/components/ui/textarea/index.js";
    import { PUBLIC_API_URL } from "$env/static/public";
    import MediaUploader from "$lib/components/mediaUploader.svelte";
    import ObjectAttributes from "$lib/components/objectAttributes.svelte";
    import MysteryObjectSubParts from "$lib/components/mysteryObjectSubParts.svelte";
    import { onMount } from 'svelte';
    import { getAuthHeader } from '$lib/utils/auth';
    import { processMediaFiles } from '$lib/utils/mediaUtils';
    import { fetchTagLabels } from '$lib/utils/fetchTags'; // Import the fetch function

    export let data;
    
    // Form state variables
    let mediaFiles = [];
    let title = '';
    let tags = [];
    let labels = [];
    let postedBy = '';
    let description = '';
    let mysteryObjectSubParts = [];
    let originalPost = null;
    let mysteryObjectId = null;
    let postId = null;
    let isLoading = true;
    let saveLoading = false;
    let errors = {
        title: '',
        media: '',
        description: '',
        auth: '',
        api: '',
        mediaUpload: '',
        subparts: ''
    };
    
    // Add an error notification state
    let showErrorNotification = false;
    let errorNotificationMessage = '';
    
    // Store all attribute values in a single object
    let attributeValues = {
        material: '',
        shape: '',
        color: '', 
        texture: '',
        weight: '',
        smell: '',
        taste: '',
        marking: '',
        functionality: '',
        period: '',
        location: '',
        writtenText: '',
        descriptionOfParts: '',
        hardness: '',
        value: '',
        originOfAcquisition: '',
        pattern: '',
        brand: '',
        print: '',
        imageLicenses: '',
        sizeX: '',
        sizeY: '',
        sizeZ: '',
        itemCondition: '',
        handmade: false,
        oneOfAKind: false
    };
    
    // Track which attributes are currently shown
    let activeAttributes = [];

    // Get current user
    $: postedBy = $activeUser;

    // Load post data on mount
    onMount(async () => {
        try {
            isLoading = true;
            postId = data.id;
            
            // Fetch post details
            const response = await fetch(`${PUBLIC_API_URL}/api/posts/getForPostDetails/${postId}`, {
                headers: getAuthHeader()
            });
            
            if (!response.ok) {
                const errorText = await response.text();
                const status = response.status;
                let userFriendlyMessage;
                
                // Translate technical errors into user-friendly messages
                if (status === 404) {
                    userFriendlyMessage = "We couldn't find this post. It may have been removed or you don't have permission to view it.";
                } else if (status === 401 || status === 403) {
                    userFriendlyMessage = "You don't have permission to edit this post. Please check if you're logged in correctly.";
                } else {
                    userFriendlyMessage = "We couldn't load this post. Please try again later.";
                }
                
                throw new Error(userFriendlyMessage);
            }
            
            const postData = await response.json();
            originalPost = postData;
            
            // Check if current user is the author
            if (postData.author !== $activeUser) {
                errors.auth = "You don't have permission to edit this post. Only the original author can make changes.";
                goto(`/thread/${postId}`);
                return;
            }
            
            // Fill in form fields with post data
            title = postData.title || '';
            description = postData.description || '';
            
            // Fetch tag labels using the IDs from postData
            if (Array.isArray(postData.tags) && postData.tags.length > 0) {
                tags = await fetchTagLabels(postData.tags); // Fetch full tag objects
            } else {
                tags = []; // Ensure tags is an empty array if no tags exist
            }
            
            // Process media files
            if (postData.mysteryObject) {
                mediaFiles = processMediaFiles(postData);
                mysteryObjectId = postData.mysteryObject.id;
                
                // Fill in mystery object attributes
                const mo = postData.mysteryObject;
                
                // Update attribute values with mystery object data
                attributeValues = {
                    material: mo.material || '',
                    shape: mo.shape || '',
                    color: mo.color || '',
                    colorHex: mo.color && mo.color.startsWith('#') ? mo.color : '',
                    texture: mo.texture || '',
                    weight: mo.weight ? mo.weight.toString() : '',
                    smell: mo.smell || '',
                    taste: mo.taste || '',
                    marking: mo.markings || '',
                    functionality: mo.functionality || '',
                    period: mo.timePeriod || '',
                    location: mo.location || '',
                    writtenText: mo.writtenText || '',
                    descriptionOfParts: mo.descriptionOfParts || '',
                    hardness: mo.hardness || '',
                    value: mo.value ? mo.value.toString() : '',
                    originOfAcquisition: mo.originOfAcquisition || '',
                    pattern: mo.pattern || '',
                    brand: mo.brand || '',
                    print: mo.print || '',
                    imageLicenses: mo.imageLicenses || '',
                    sizeX: mo.sizeX ? mo.sizeX.toString() : '',
                    sizeY: mo.sizeY ? mo.sizeY.toString() : '',
                    sizeZ: mo.sizeZ ? mo.sizeZ.toString() : '',
                    itemCondition: mo.item_condition || '',
                    handmade: mo.handmade || false,
                    oneOfAKind: mo.oneOfAKind || false
                };
                
                // Set active attributes based on what's filled in
                activeAttributes = Object.keys(attributeValues).filter(key => {
                    const value = attributeValues[key];
                    return value !== '' && value !== null && value !== undefined;
                });
                
                // Load sub-parts
                if (mo.subParts && Array.isArray(mo.subParts)) {
                    mysteryObjectSubParts = mo.subParts;
                }
            }
        } catch (error) {
            console.error('Error loading post for editing:', error);
            errors.auth = error.message || "We couldn't load your post. Please refresh the page or try again later.";
            
            // Show notification for 5 seconds
            errorNotificationMessage = error.message || "Failed to load post data. Please try refreshing the page.";
            showErrorNotification = true;
            setTimeout(() => { showErrorNotification = false; }, 5000);
        } finally {
            isLoading = false;
        }
    });

    // Function to handle file selection from MediaUploader
    function handleMediaUpdate(event) {
        mediaFiles = event.detail.mediaFiles;
    }
    
    // Handle updates from ObjectAttributes component
    function handleAttributesUpdate(event) {
        activeAttributes = event.detail.activeAttributes;
    }
    
    // Handle attribute value changes
    function handleAttributeValueChange(event) {
        attributeValues = event.detail.attributeValues;
    }
    
    // Handle updates from the MysteryObjectSubParts component
    function handleSubPartsUpdate(event) {
        mysteryObjectSubParts = event.detail.subParts;
    }
    
    // Function to create a clean subpart object suitable for the API
    function prepareSubPartForApi(subpart) {
        return {
            id: subpart.id, // Include ID for existing subparts
            description: subpart.description || "",
            material: subpart.material || null,
            color: subpart.color || null,
            shape: subpart.shape || null,
            location: subpart.location || null,
            smell: subpart.smell || null,
            taste: subpart.taste || null,
            texture: subpart.texture || null,
            functionality: subpart.functionality || null,
            markings: subpart.markings || null,
            handmade: subpart.handmade || false,
            oneOfAKind: subpart.oneOfAKind || false,
            weight: subpart.weight ? parseFloat(subpart.weight) : null,
            timePeriod: subpart.timePeriod || null,
            writtenText: subpart.writtenText || null,
            descriptionOfParts: subpart.descriptionOfParts || null,
            hardness: subpart.hardness || null,
            value: subpart.value ? parseFloat(subpart.value) : null,
            originOfAcquisition: subpart.originOfAcquisition || null,
            pattern: subpart.pattern || null,
            brand: subpart.brand || null,
            print: subpart.print || null,
            imageLicenses: subpart.imageLicenses || null,
            sizeX: subpart.sizeX ? parseFloat(subpart.sizeX) : null,
            sizeY: subpart.sizeY ? parseFloat(subpart.sizeY) : null,
            sizeZ: subpart.sizeZ ? parseFloat(subpart.sizeZ) : null,
            item_condition: subpart.item_condition || null
        };
    }

    // References to form fields for error scrolling
    let titleRef;
    let descriptionRef;
    let mediaRef;
    
    // Function to scroll to the first field with an error
    function scrollToFirstError() {
        // Check each field in order of appearance
        if (errors.title && titleRef) {
            titleRef.scrollIntoView({ behavior: 'smooth', block: 'center' });
            return;
        }
        
        if (errors.description && descriptionRef) {
            descriptionRef.scrollIntoView({ behavior: 'smooth', block: 'center' });
            return;
        }
        
        if (errors.media && mediaRef) {
            mediaRef.scrollIntoView({ behavior: 'smooth', block: 'center' });
            return;
        }
        
        // If no specific field errors, but there are API errors, scroll to the top
        if (errors.api || errors.auth || errors.mediaUpload || errors.subparts) {
            window.scrollTo({ top: 0, behavior: 'smooth' });
            return;
        }
    }

    // Handle post update form submission
    async function handleUpdate() {
        // Reset error state
        errors = {
            title: '',
            media: '',
            description: '',
            auth: '',
            api: '',
            mediaUpload: '',
            subparts: ''
        };
        
        // Validate required fields
        let hasErrors = false;
        
        if (!title.trim()) {
            errors.title = 'Title is required';
            hasErrors = true;
        }
        
        if (mediaFiles.length === 0) {
            errors.media = 'At least one media file is required';
            hasErrors = true;
        }
        
        if (!description.trim()) {
            errors.description = 'Description is required';
            hasErrors = true;
        }
        
        if (hasErrors) {
            // Scroll to the first error field
            setTimeout(scrollToFirstError, 100);
            return; // Don't proceed if validation fails
        }

        saveLoading = true;
        
        try {
            // Deep clone and sanitize attribute values to prevent JSON issues
            const sanitizedAttributes = JSON.parse(JSON.stringify(attributeValues));

            // Create mysteryObject using sanitized attributes
            const mysteryObject = {
                id: mysteryObjectId, // Important: Include ID for update
                description,
                material: sanitizedAttributes.material || null,
                color: sanitizedAttributes.colorHex || sanitizedAttributes.color || null,
                shape: sanitizedAttributes.shape || null,
                location: sanitizedAttributes.location || null,
                smell: sanitizedAttributes.smell || null,
                taste: sanitizedAttributes.taste || sanitizedAttributes.smell || null,
                texture: sanitizedAttributes.texture || null,
                functionality: sanitizedAttributes.functionality || null,
                markings: sanitizedAttributes.marking || null,
                handmade: sanitizedAttributes.handmade || false,
                oneOfAKind: sanitizedAttributes.oneOfAKind || false,
                weight: parseFloat(sanitizedAttributes.weight) || null,
                timePeriod: sanitizedAttributes.period || null,
                writtenText: sanitizedAttributes.writtenText || null,
                descriptionOfParts: sanitizedAttributes.descriptionOfParts || null,
                hardness: sanitizedAttributes.hardness || null,
                value: parseFloat(sanitizedAttributes.value) || null,
                originOfAcquisition: sanitizedAttributes.originOfAcquisition || null,
                pattern: sanitizedAttributes.pattern || null,
                brand: sanitizedAttributes.brand || null,
                print: sanitizedAttributes.print || null,
                imageLicenses: sanitizedAttributes.imageLicenses || null,
                sizeX: parseFloat(sanitizedAttributes.sizeX) || null,
                sizeY: parseFloat(sanitizedAttributes.sizeY) || null,
                sizeZ: parseFloat(sanitizedAttributes.sizeZ) || null,
                item_condition: sanitizedAttributes.itemCondition || null
            };

            // Create update payload
            const updatePayload = {
                id: postId, // Include post ID for update
                title,
                content: description,
                tags: tags.map(tag => tag.id),
                mysteryObject: mysteryObject
            };

            // Make the API call to update the post
            const updateResponse = await fetch(`${PUBLIC_API_URL}/api/posts/update`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    ...getAuthHeader()
                },
                body: JSON.stringify(updatePayload)
            });

            if (!updateResponse.ok) {
                const errorText = await updateResponse.text();
                const status = updateResponse.status;
                
                // Translate status codes to user-friendly messages
                if (status === 401 || status === 403) {
                    throw new Error("Your session may have expired. Please try logging in again.");
                } else if (status === 413) {
                    throw new Error("Your post couldn't be updated because it contains files that are too large.");
                } else if (status === 400) {
                    throw new Error("Some information in your post isn't valid. Please check all fields and try again.");
                } else {
                    throw new Error("Your changes couldn't be saved at this time. Please try again later.");
                }
            }

            const responseData = await updateResponse.json();
            console.log("Post updated successfully:", responseData);

            // Update main image if needed
            if (mediaFiles.length > 0 && mediaFiles[0].file) {
                try {
                    const imageFormData = new FormData();
                    imageFormData.append('file', mediaFiles[0].file);

                    const imageResponse = await fetch(`${PUBLIC_API_URL}/api/posts/${postId}/mysteryObjects/${mysteryObjectId}/set-image`, {
                        method: 'POST',
                        headers: getAuthHeader(),
                        body: imageFormData
                    });
                    
                    if (!imageResponse.ok) {
                        const errorText = await imageResponse.text();
                        throw new Error(`We couldn't update the main image, but other changes were saved.`);
                    }
                } catch (mediaError) {
                    console.error('Error uploading main image:', mediaError);
                    errors.mediaUpload = "The main image couldn't be updated, but your other changes were saved successfully.";
                    
                    // Continue with other operations despite main image failure
                    errorNotificationMessage = "Post updated but we couldn't change the main image. You can try again.";
                    showErrorNotification = true;
                    setTimeout(() => { showErrorNotification = false; }, 5000);
                }
            }

            // Handle media files upload (for new files only)
            let mediaUploadErrors = [];
            for (let i = 0; i < mediaFiles.length; i++) {
                const mediaItem = mediaFiles[i];
                
                // Skip files that don't have a file property (existing files)
                if (!mediaItem.file) continue;
                
                try {
                    const mediaFormData = new FormData();
                    mediaFormData.append('file', mediaItem.file);
                    mediaFormData.append('type', mediaItem.type || 'image');

                    const mediaResponse = await fetch(`${PUBLIC_API_URL}/api/mysteryObjects/${mysteryObjectId}/upload-media`, {
                        method: 'POST',
                        headers: getAuthHeader(),
                        body: mediaFormData
                    });
                    
                    if (!mediaResponse.ok) {
                        const errorText = await mediaResponse.text();
                        throw new Error(`We couldn't upload this media file. It may be too large or in an unsupported format.`);
                    }
                } catch (mediaError) {
                    console.error(`Error uploading media file ${i+1}:`, mediaError);
                    mediaUploadErrors.push(`Media file ${i+1}: Couldn't be added to your post`);
                }
            }
            
            if (mediaUploadErrors.length > 0) {
                errors.mediaUpload = `We saved your post but couldn't add ${mediaUploadErrors.length} new media file${mediaUploadErrors.length === 1 ? '' : 's'}.`;
                errorNotificationMessage = "Post updated but some media files weren't added. You can try adding them again.";
                showErrorNotification = true;
                setTimeout(() => { showErrorNotification = false; }, 5000);
            }

            // Handle sub-parts updates
            let subpartErrors = [];
            if (mysteryObjectSubParts.length > 0) {
                // For each sub-part: update existing ones, create new ones
                for (let i = 0; i < mysteryObjectSubParts.length; i++) {
                    try {
                        const subPart = mysteryObjectSubParts[i];
                        const cleanSubPart = prepareSubPartForApi(subPart);
                        
                        const endpoint = subPart.id 
                            ? `${PUBLIC_API_URL}/api/mysteryObjects/${mysteryObjectId}/subParts/${subPart.id}`
                            : `${PUBLIC_API_URL}/api/mysteryObjects/${mysteryObjectId}/subParts`;
                        const method = subPart.id ? 'PUT' : 'POST';
                        
                        const subPartResponse = await fetch(endpoint, {
                            method: method,
                            headers: {
                                'Content-Type': 'application/json',
                                ...getAuthHeader()
                            },
                            body: JSON.stringify(cleanSubPart)
                        });
                        
                        if (!subPartResponse.ok) {
                            const errorText = await subPartResponse.text();
                            const action = method === 'POST' ? 'add' : 'update';
                            throw new Error(`We couldn't ${action} this part. Please check the information you provided.`);
                        }
                    } catch (subpartError) {
                        console.error(`Error handling sub-part ${i+1}:`, subpartError);
                        const partName = mysteryObjectSubParts[i].description || `Part ${i+1}`;
                        subpartErrors.push(`"${partName}": Couldn't be saved`);
                    }
                }
                
                // Find deleted sub-parts by comparing with original sub-parts
                if (originalPost?.mysteryObject?.subParts) {
                    const originalSubPartIds = new Set(originalPost.mysteryObject.subParts.map(p => p.id));
                    const currentSubPartIds = new Set(mysteryObjectSubParts.filter(p => p.id).map(p => p.id));
                    
                    // Find IDs that were in the original but not in current -> these need to be deleted
                    for (const id of originalSubPartIds) {
                        if (!currentSubPartIds.has(id)) {
                            try {
                                // Delete this sub-part
                                const deleteResponse = await fetch(`${PUBLIC_API_URL}/api/mysteryObjects/${mysteryObjectId}/subParts/${id}`, {
                                    method: 'DELETE',
                                    headers: getAuthHeader()
                                });
                                
                                if (!deleteResponse.ok) {
                                    const errorText = await deleteResponse.text();
                                    throw new Error(`Failed to delete sub-part: ${errorText}`);
                                }
                            } catch (deleteError) {
                                console.error(`Error deleting sub-part ${id}:`, deleteError);
                                subpartErrors.push(`Delete sub-part: ${deleteError.message}`);
                            }
                        }
                    }
                }
            }
            
            if (subpartErrors.length > 0) {
                errors.subparts = `We saved your post but ${subpartErrors.length} object part${subpartErrors.length === 1 ? '' : 's'} couldn't be updated.`;
                errorNotificationMessage = "Post updated but some object parts weren't saved. You can try updating them again.";
                showErrorNotification = true;
                setTimeout(() => { showErrorNotification = false; }, 5000);
            }

            // If there were some non-critical errors but post was updated, show them but still navigate
            if (errors.mediaUpload || errors.subparts) {
                setTimeout(() => goto(`/thread/${postId}`), 3000); // Navigate after a short delay to show error
            } else {
                // Navigate back to the thread page immediately if everything succeeded
                goto(`/thread/${postId}`);
            }
        } catch (error) {
            console.error('Error updating post:', error);
            errors.api = error.message || "We couldn't update your post at this time. Please try again later.";
            errorNotificationMessage = error.message || "Failed to update post";
            showErrorNotification = true;
            setTimeout(() => { showErrorNotification = false; }, 5000);
        } finally {
            saveLoading = false;
        }
    }
</script>

<div class="flex flex-col items-center bg-change dark:bg-dark shifting p-3 py-5">
    <div class="w-full max-w-7xl mx-auto">
        <!-- Error notification toast with improved messaging -->
        {#if showErrorNotification}
            <div class="fixed top-5 right-5 z-50 bg-red-100 dark:bg-red-900/30 border border-red-200 dark:border-red-800/50 text-red-800 dark:text-red-300 px-4 py-3 rounded-lg shadow-lg flex items-center animate-slide-in">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
                </svg>
                {errorNotificationMessage}
                <button 
                    class="ml-3 text-red-800 dark:text-red-300 hover:text-red-900 dark:hover:text-red-200"
                    on:click={() => showErrorNotification = false}
                >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                    </svg>
                </button>
            </div>
        {/if}
        
        <form class="w-full" on:submit|preventDefault={handleUpdate}>
            <Card.Root class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden">
                <Card.Header class="p-4 border-b border-neutral-100 dark:border-neutral-800">
                    <Card.Title class="text-xl font-semibold text-neutral-900 dark:text-white">
                        Edit your post
                    </Card.Title>
                    <Card.Description class="text-sm mt-1 text-neutral-600 dark:text-neutral-400">
                        Update the details about your mystery object
                    </Card.Description>
                </Card.Header>

                {#if isLoading}
                    <div class="flex justify-center items-center py-16">
                        <div class="inline-block h-10 w-10 border-4 border-neutral-200 dark:border-neutral-800 border-t-teal-600 dark:border-t-teal-500 rounded-full animate-spin"></div>
                    </div>
                {:else}
                    <Card.Content class="p-4 sm:p-6">
                        <!-- Authentication error with improved messaging -->
                        {#if errors.auth}
                            <div class="bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300 p-3 rounded-md mb-4 border border-red-200 dark:border-red-800/50">
                                <div class="flex items-center">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2 flex-shrink-0" viewBox="0 0 20 20" fill="currentColor">
                                        <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
                                    </svg>
                                    {errors.auth}
                                </div>
                                <p class="text-xs text-red-700 dark:text-red-400 mt-2 ml-7">If you think this is a mistake, try logging out and logging back in.</p>
                            </div>
                        {/if}
                        
                        <!-- API error with improved messaging -->
                        {#if errors.api}
                            <div class="bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300 p-3 rounded-md mb-4 border border-red-200 dark:border-red-800/50">
                                <div class="flex items-center">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2 flex-shrink-0" viewBox="0 0 20 20" fill="currentColor">
                                        <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
                                    </svg>
                                    {errors.api}
                                </div>
                                <p class="text-xs text-red-700 dark:text-red-400 mt-2 ml-7">You might want to check your internet connection or try again in a few minutes.</p>
                            </div>
                        {/if}
                        
                        <!-- Media upload errors with improved messaging -->
                        {#if errors.mediaUpload}
                            <div class="bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-300 p-3 rounded-md mb-4 border border-yellow-200 dark:border-yellow-800/50">
                                <div class="flex items-center">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2 flex-shrink-0" viewBox="0 0 20 20" fill="currentColor">
                                        <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
                                    </svg>
                                    {errors.mediaUpload}
                                </div>
                                <p class="text-xs text-yellow-700 dark:text-yellow-400 mt-1 ml-7">The rest of your changes have been saved. You can try uploading the media files again later.</p>
                            </div>
                        {/if}
                        
                        <!-- Subparts errors with improved messaging -->
                        {#if errors.subparts}
                            <div class="bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-300 p-3 rounded-md mb-4 border border-yellow-200 dark:border-yellow-800/50">
                                <div class="flex items-center">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2 flex-shrink-0" viewBox="0 0 20 20" fill="currentColor">
                                        <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
                                    </svg>
                                    {errors.subparts}
                                </div>
                                <p class="text-xs text-yellow-700 dark:text-yellow-400 mt-1 ml-7">Your post has been updated with the main changes. Try simplifying the object parts if they have too many details.</p>
                            </div>
                        {/if}

                        <!-- Title -->
                        <div class="mb-4">
                            <label for="title" class="block text-sm font-medium mb-1.5 text-neutral-700 dark:text-neutral-300 flex items-center">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                                    <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd" />
                                </svg>
                                Title*
                            </label>
                            <div bind:this={titleRef}>
                                <Textarea 
                                    id="title" 
                                    class="w-full p-3 border rounded-md text-sm bg-white dark:bg-neutral-950 border-neutral-200 dark:border-neutral-700 focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500 {errors.title ? 'border-red-500 dark:border-red-800' : ''}" 
                                    bind:value={title} 
                                    placeholder="This is what people will see on their homepage so try to make it interesting"
                                />
                                {#if errors.title}
                                    <p class="text-red-500 text-sm mt-1">{errors.title}</p>
                                {/if}
                            </div>
                        </div>

                        <!-- Description -->
                        <div class="mb-5">
                            <label for="description" class="block text-sm font-medium mb-1.5 text-neutral-700 dark:text-neutral-300 flex items-center">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                                    <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 2a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7zm0 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd" />
                                </svg>
                                Description*
                            </label>
                            <div bind:this={descriptionRef}>
                                <Textarea 
                                    id="description" 
                                    class="w-full p-3 border rounded-md text-sm bg-white dark:bg-neutral-950 border-neutral-200 dark:border-neutral-700 focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500 min-h-[120px] {errors.description ? 'border-red-500 dark:border-red-800' : ''}" 
                                    bind:value={description} 
                                    placeholder="You can add any additional context about your object or how you came into possession of it."
                                />
                                {#if errors.description}
                                    <p class="text-red-500 text-sm mt-1">{errors.description}</p>
                                {/if}
                            </div>
                        </div>
                        
                        <!-- Attributes and Tags wrapper for side-by-side on lg screens -->
                        <div class="flex flex-col lg:flex-row lg:gap-6 mb-6">
                            <!-- Object Attributes Component -->
                            <div class="w-full lg:w-1/2 mb-6 lg:mb-0">
                                <div class="mb-2 text-sm font-medium text-neutral-700 dark:text-neutral-300 flex items-center">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                                        <path fill-rule="evenodd" d="M17.707 9.293a1 1 0 010 1.414l-7 7a1 1 0 01-1.414 0l-7-7A.997.997 0 012 10V5a3 3 0 013-3h5c.256 0 .512.098.707.293l7 7zM5 6a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                                    </svg>
                                    Object Attributes
                                    <span class="ml-2 px-2 py-0.5 bg-neutral-100 dark:bg-neutral-800 text-xs rounded-full text-neutral-700 dark:text-neutral-300">
                                        {activeAttributes.length} added
                                    </span>
                                </div>
                                <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-sm p-3">
                                    <ObjectAttributes 
                                        bind:attributeValues={attributeValues}
                                        bind:activeAttributes={activeAttributes}
                                        on:update={handleAttributesUpdate}
                                        on:valuechange={handleAttributeValueChange}
                                    />
                                </div>
                            </div>

                            <!-- Tags -->
                            <div class="w-full lg:w-1/2">
                                <div class="mb-2 text-sm font-medium text-neutral-700 dark:text-neutral-300 flex items-center">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                                        <path fill-rule="evenodd" d="M17.707 9.293a1 1 0 010 1.414l-7 7a1 1 0 01-1.414 0l-7-7A.997.997 0 012 10V5a3 3 0 013-3h5c.256 0 .512.098.707.293l7 7zM5 6a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                                    </svg>
                                    Tags
                                    <span class="ml-2 px-2 py-0.5 bg-neutral-100 dark:bg-neutral-800 text-xs rounded-full text-neutral-700 dark:text-neutral-300">
                                        {tags.length} added
                                    </span>
                                </div>
                                <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-sm p-3">
                                    <Query bind:tags={tags} bind:labels={labels} />
                                </div>
                            </div>
                        </div>

                        <!-- Sub-parts component -->
                        <div class="mb-6">
                            <h3 class="block text-sm font-medium mb-2 text-neutral-700 dark:text-neutral-300 flex items-center">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                                    <path d="M7 3a1 1 0 000 2h6a1 1 0 100-2H7zM4 7a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1zM2 11a2 2 0 012-2h12a2 2 0 012 2v4a2 2 0 01-2 2H4a2 2 0 01-2-2v-4z" />
                                </svg>
                                Object Parts
                            </h3>
                            <p class="text-sm text-neutral-500 dark:text-neutral-400 mb-3">You can add parts to your mystery object if it consists of multiple components.</p>

                            <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-sm p-3">
                                <MysteryObjectSubParts
                                    mysteryObjectId={mysteryObjectId}
                                    bind:subParts={mysteryObjectSubParts}
                                    on:update={handleSubPartsUpdate}
                                />
                            </div>
                        </div>

                        <!-- Media upload component -->
                        <div class="mb-6">
                            <h3 class="block text-sm font-medium mb-2 text-neutral-700 dark:text-neutral-300 flex items-center">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                                    <path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd" />
                                </svg>
                                Media Files*
                            </h3>
                            <div bind:this={mediaRef} class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-sm p-3 {errors.media ? 'border-red-500 dark:border-red-800' : ''}">
                                <MediaUploader
                                    bind:mediaFiles={mediaFiles}
                                    bind:errors={errors}
                                    on:update={handleMediaUpdate}
                                />
                                {#if errors.media}
                                    <p class="text-red-500 text-sm mt-2">{errors.media}</p>
                                {/if}
                            </div>
                        </div>
                    </Card.Content>

                    <!-- Action Buttons - Updated with consistent padding and placement -->
                    <div class="flex justify-start gap-3 border-t border-neutral-100 dark:border-neutral-800 p-4 sm:p-6 pt-4">
                        <Button
                            on:click={handleUpdate}
                            variant="default" 
                            disabled={saveLoading}
                            class="text-sm py-1 px-4 bg-teal-600 hover:bg-teal-700 text-white rounded-full"
                        >
                            {#if saveLoading}
                                <div class="inline-block h-3.5 w-3.5 mr-1.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                Saving...
                            {:else}
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                                    <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                                </svg>
                                Save Changes
                            {/if}
                        </Button>
                        <Button
                            variant="outline"
                            on:click={() => goto(`/thread/${postId}`)}
                            class="text-sm py-1 px-3 border-neutral-300 dark:border-neutral-700 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-full"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm.707-10.293a1 1 0 00-1.414-1.414l-3 3a1 1 0 000 1.414l3 3a1 1 0 001.414-1.414L9.414 11H13a1 1 0 100-2H9.414l1.293-1.293z" clip-rule="evenodd" />
                            </svg>
                            Cancel
                        </Button>
                    </div>
                {/if}
            </Card.Root>
        </form>
    </div>
</div>

<style>
    /* Matching animations from the thread page */
    @keyframes fadeIn {
        from { opacity: 0; transform: translateY(8px); }
        to { opacity: 1; transform: translateY(0); }
    }
    
    @keyframes slide-in {
        from { transform: translateX(100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
    }
    
    form {
        animation: fadeIn 0.4s ease-out;
    }
    
    .animate-slide-in {
        animation: slide-in 0.3s ease-out;
    }
    
    /* Media query for better mobile display */
    @media (max-width: 640px) {
        .flex-col-mobile {
            flex-direction: column !important;
        }
        
        .w-full-mobile {
            width: 100% !important;
        }
    }
</style>
