<script>
    import * as Card from "$lib/components/ui/card/index.js";
    import { Button } from "$lib/components/ui/button";
    import Query from '$lib/components/query.svelte';
    import { threadStore } from "../../threadStore";
    import { goto } from '$app/navigation';
    import { activeUser } from '../../userStore';
    import { Textarea } from "$lib/components/ui/textarea/index.js";
    import { PUBLIC_API_URL } from "$env/static/public";
    import MediaUploader from "$lib/components/mediaUploader.svelte";
    import ObjectAttributes from "$lib/components/objectAttributes.svelte";
    import MysteryObjectSubParts from "$lib/components/mysteryObjectSubParts.svelte";

    // Replace single imageFile with array of media files
    let mediaFiles = [];
    let title = '';
    let tags = [];
    let labels = [];
    let postedBy;
    let description = '';
    let anonymous = false; 
    let resolved = false;
    
    // For mystery object sub-parts
    let mysteryObjectSubParts = [];

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
    
    let errors = {
        title: '',
        media: '',
        description: ''
    };

    $: postedBy = $activeUser;

    async function fetchLabelForQcode(qcode) {
        const wikidataApiUrl = `https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${qcode}&format=json&languages=en&origin=*`;
        try {
            const response = await fetch(wikidataApiUrl);
            const data = await response.json();
            const label = data.entities[qcode]?.labels?.en?.value;
            if (!label) throw new Error(`Label not found for qcode: ${qcode}`);
            return label;
        } catch (error) {
            console.error('Error fetching label:', error);
            return null;
        }
    }

    async function enrichTagsWithLabels(tags) {
        const enrichedTags = [];
        for (const tag of tags) {
            const label = await fetchLabelForQcode(tag.id);
            if (label) {
                enrichedTags.push({ id: tag.id, label });
            }
        }
        return enrichedTags;
    }
    
    // Handle updates from the MediaUploader component
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
    
    // Function to create a clean subpart object suitable for the API
    function prepareSubPartForApi(subpart) {
        return {
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

    // Add loading state and more comprehensive error tracking
    let isSubmitting = false;
    let formErrors = {
        general: '',
        api: '',
        imageUpload: '',
        mediaFiles: [],
        subParts: []
    };

    let handlePost = async () => {
        // Reset error state
        errors = {
            title: '',
            media: '',
            description: ''
        };
        
        // Reset submission errors
        formErrors = {
            general: '',
            api: '',
            imageUpload: '',
            mediaFiles: [],
            subParts: []
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
            // Scroll to the top of the form to show validation errors
            window.scrollTo({ top: 0, behavior: 'smooth' });
            return; // Don't proceed if validation fails
        }

        try {
            isSubmitting = true; // Start loading state
            const enrichedTags = await enrichTagsWithLabels(tags);
            
            // Deep clone and sanitize attribute values to prevent JSON issues
            const sanitizedAttributes = JSON.parse(JSON.stringify(attributeValues));

            // Create mysteryObject using sanitized attributes
            const mysteryObject = {
                description,
                material: sanitizedAttributes.material || null,
                // Fix: Only use the hex color value without additional properties
                color: sanitizedAttributes.colorHex || null,
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
            
            // Store color name as a UI-side property that doesn't go to the backend
            // This will be handled separately by the Post component
            const colorNameForUI = attributeValues.color;

            // Create FormData with better error handling
            const formData = new FormData();
            formData.append('title', title);
            formData.append('content', description);
            
            // Add tags
            if (tags && tags.length > 0) {
                const tagIds = tags.map(tag => tag.id);
                formData.append('tags', JSON.stringify(tagIds));
            } else {
                // Add empty array if no tags to avoid null issues
                formData.append('tags', JSON.stringify([]));
            }
            
            // Add mystery object as JSON string (without subparts)
            formData.append('mysteryObject', JSON.stringify(mysteryObject));
            
            // Add first media file as the primary image
            if (mediaFiles.length > 0) {
                const firstFile = mediaFiles[0].file;
                formData.append('image', firstFile);
            }

            // Create a simplified post with just the essential data to avoid issues
            const simplifiedPost = {
                title,
                content: description,
                tags: tags.map(tag => tag.id),
                mysteryObject: mysteryObject
            };

            // First create the post without files
            // console.log("Creating post with JSON data");
            const createResponse = await fetch(`${PUBLIC_API_URL}/api/posts/create-json`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(simplifiedPost),
                credentials: 'include'
            });

            if (!createResponse.ok) {
                let errorMessage = '';
                try {
                    const errorData = await createResponse.json();
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    } else if (errorData.error) {
                        errorMessage = errorData.error;
                    }
                    
                    // Convert technical messages to user-friendly ones
                    if (errorMessage.includes('401') || errorMessage.includes('Unauthorized')) {
                        formErrors.api = 'Your session may have expired. Please log in again.';
                    } else if (errorMessage.includes('413') || errorMessage.toLowerCase().includes('large')) {
                        formErrors.api = 'Your image is too large. Please try with a smaller file size.';
                    } else if (errorMessage.includes('403') || errorMessage.includes('Forbidden')) {
                        formErrors.api = 'You don\'t have permission to create this post.';
                    } else {
                        formErrors.api = 'We couldn\'t create your post. Please check your information and try again.';
                    }
                } catch (parseError) {
                    console.error("Error parsing response:", parseError);
                    formErrors.api = 'Something went wrong. Please try again or check your connection.';
                }
                throw new Error(formErrors.api);
            }

            const responseData = await createResponse.json();
            // console.log("Post created successfully:", responseData);

            // Then upload image separately if available
            if (mediaFiles.length > 0) {
                const imageFormData = new FormData();
                imageFormData.append('file', mediaFiles[0].file);

                // console.log("Uploading main image");
                try {
                    const imageResponse = await fetch(`${PUBLIC_API_URL}/api/posts/${responseData.postId}/mysteryObjects/${responseData.mysteryObjectId}/set-image`, {
                        method: 'POST',
                        body: imageFormData,
                        credentials: 'include'
                    });
                    
                    if (!imageResponse.ok) {
                        formErrors.imageUpload = 'We couldn\'t upload the main image, but your post was created. You can try adding the image later.';
                        console.error("Error uploading main image:", await imageResponse.text());
                    }
                } catch (imageError) {
                    formErrors.imageUpload = 'Image upload failed, but your post was created. You can add images later from the edit screen.';
                    console.error("Error uploading main image:", imageError);
                }
            }

            // Upload media files
            let mediaUploadErrors = 0;
            if (mediaFiles.length > 0) {
                // console.log(`Uploading ${mediaFiles.length} media files`);

                for (let i = 0; i < mediaFiles.length; i++) {
                    const mediaItem = mediaFiles[i];
                    const mediaFormData = new FormData();
                    mediaFormData.append('file', mediaItem.file);
                    mediaFormData.append('type', mediaItem.type || 'image');

                    try {
                        // console.log(`Uploading media file ${i+1}/${mediaFiles.length}`);
                        const mediaResponse = await fetch(`${PUBLIC_API_URL}/api/mysteryObjects/${responseData.mysteryObjectId}/upload-media`, {
                            method: 'POST',
                            body: mediaFormData,
                            credentials: 'include'
                        });

                        if (!mediaResponse.ok) {
                            formErrors.mediaFiles.push({
                                index: i,
                                filename: mediaItem.file.name,
                                error: `Failed to upload. The file may be too large or in an unsupported format.`
                            });
                            mediaUploadErrors++;
                            console.error(`Failed to upload media file ${i+1}:`, mediaResponse.status, mediaResponse.statusText);
                        } else {
                            // console.log(`Media file ${i+1} uploaded successfully`);
                        }
                    } catch (mediaError) {
                        formErrors.mediaFiles.push({
                            index: i,
                            filename: mediaItem.file.name,
                            error: `Couldn't upload. Please check your connection and try again.`
                        });
                        mediaUploadErrors++;
                        console.error(`Error uploading media file ${i+1}:`, mediaError);
                    }
                }
            }

            // Add sub-parts to the mystery object
            let subPartErrors = 0;
            if (mysteryObjectSubParts.length > 0) {
                // console.log(`Adding ${mysteryObjectSubParts.length} sub-parts to mystery object`);

                // Process sub-parts sequentially to avoid race conditions
                for (let i = 0; i < mysteryObjectSubParts.length; i++) {
                    try {
                        // console.log(`Adding sub-part ${i+1}/${mysteryObjectSubParts.length}`);

                        // Create a clean object suitable for the API
                        const cleanSubPart = prepareSubPartForApi(mysteryObjectSubParts[i]);

                        const subPartResponse = await fetch(`${PUBLIC_API_URL}/api/mysteryObjects/${responseData.mysteryObjectId}/subParts`, {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json',
                            },
                            body: JSON.stringify(cleanSubPart),
                            credentials: 'include'
                        });

                        if (!subPartResponse.ok) {
                            subPartErrors++;
                            formErrors.subParts.push({
                                index: i,
                                name: mysteryObjectSubParts[i].description || `Part ${i+1}`,
                                error: `Could not add this part. Please check if your description is complete.`
                            });
                            console.error(`Failed to add sub-part ${i+1}:`, subPartResponse.status, subPartResponse.statusText);
                            const errorText = await subPartResponse.text();
                            console.error('Error response:', errorText);
                        } else {
                            // console.log(`Sub-part ${i+1} added successfully`);
                        }
                    } catch (subPartError) {
                        subPartErrors++;
                        formErrors.subParts.push({
                            index: i,
                            name: mysteryObjectSubParts[i].description || `Part ${i+1}`,
                            error: `Failed to add this part. Try again with fewer details.`
                        });
                        console.error(`Error adding sub-part ${i+1}:`, subPartError);
                    }
                }
            }
            
            // Even with some errors, if the post was created we can navigate away
            // But only if there are no critical errors (like post creation failure)
            if (mediaUploadErrors > 0 || subPartErrors > 0) {
                // Some attachments failed, but the post was created
                let errorCount = mediaUploadErrors + subPartErrors;
                formErrors.general = `Your post was created successfully! However, ${errorCount} attachment${errorCount === 1 ? '' : 's'} couldn't be added. You can edit your post later to add them.`;
                // Give the user time to see the error before redirecting
                setTimeout(() => {
                    // Update thread store and navigate to home
                    threadStore.update(prev => [...prev, responseData]);
                    goto('/');
                }, 4000);
            } else {
                // All good, update thread store and navigate immediately
                threadStore.update(prev => [...prev, responseData]);
                goto('/');
            }
            
        } catch (error) {
            console.error('Error creating post:', error);
            if (!formErrors.general && !formErrors.api) {
                formErrors.general = 'We couldn\'t create your post. Please check your information and try again later.';
            }
            // Scroll to the top to show the error
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } finally {
            isSubmitting = false; // End loading state
        }
    };

    // Clean up object URLs when component is destroyed
    import { onDestroy } from 'svelte';
    
    onDestroy(() => {
        // Clean up any created object URLs
        for (const media of mediaFiles) {
            if (media.url) {
                URL.revokeObjectURL(media.url);
            }
        }
    });

    // Add state for color picker
    let selectedHexColor = "#ffffff";
    let isLoadingColorName = false;
    
    // Function to fetch color name from TheColorAPI
    async function fetchColorName(hexColor) {
        isLoadingColorName = true;
        // Remove the # character from hex color
        const cleanHex = hexColor.replace('#', '');
        
        try {
            const response = await fetch(`https://www.thecolorapi.com/id?hex=${cleanHex}`);
            if (!response.ok) {
                throw new Error('Failed to fetch color name');
            }
            const data = await response.json();
            
            // Update color value with the name
            attributeValues.color = data.name.value || 'Unknown color';
        } catch (error) {
            console.error('Error fetching color name:', error);
            attributeValues.color = `Color #${cleanHex}`;
        } finally {
            isLoadingColorName = false;
        }
    }
    
    function handleColorChange(event) {
        selectedHexColor = event.target.value;
        fetchColorName(selectedHexColor);
    }

    // Add this helper function for consistent removal icon
    function RemovalIcon() {
        return `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>`;
    }

    // Handle updates from the MysteryObjectSubParts component
    function handleSubPartsUpdate(event) {
        mysteryObjectSubParts = event.detail.subParts;
    }
</script>

<div class="flex flex-col items-center bg-change dark:bg-dark shifting p-3 py-5">
    <div class="w-full max-w-7xl mx-auto">
        <form class="w-full" on:submit|preventDefault={handlePost}>
            <Card.Root class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden">
                <Card.Header class="p-4 border-b border-neutral-100 dark:border-neutral-800">
                    <Card.Title class="text-xl font-semibold text-neutral-900 dark:text-white">
                        Let's help you post new stuff
                    </Card.Title>
                    <Card.Description class="text-sm mt-1 text-neutral-600 dark:text-neutral-400">
                        Fill in the details about your object, you can leave any blank if you are not sure, but try to be as precise as possible
                    </Card.Description>
                </Card.Header>

                <Card.Content class="p-4 sm:p-6">
                    <!-- Error display area with improved user-friendly messages -->
                    {#if formErrors.general || formErrors.api}
                        <div class="mb-6 p-4 border border-red-200 bg-red-50 dark:bg-red-900/20 dark:border-red-800 rounded-lg">
                            <div class="flex items-start">
                                <div class="flex-shrink-0 mt-0.5">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-red-500" viewBox="0 0 20 20" fill="currentColor">
                                        <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
                                    </svg>
                                </div>
                                <div class="ml-3">
                                    <h3 class="text-sm font-medium text-red-800 dark:text-red-300">We encountered a problem</h3>
                                    <div class="mt-1 text-sm text-red-700 dark:text-red-400">
                                        {formErrors.general || formErrors.api}
                                    </div>
                                    
                                    <!-- Show media file upload errors if any -->
                                    {#if formErrors.mediaFiles.length > 0}
                                        <div class="mt-2">
                                            <details class="text-sm">
                                                <summary class="cursor-pointer font-medium">
                                                    {formErrors.mediaFiles.length} media file issue{formErrors.mediaFiles.length === 1 ? '' : 's'}
                                                </summary>
                                                <ul class="mt-2 pl-5 list-disc space-y-1 text-xs">
                                                    {#each formErrors.mediaFiles as fileError}
                                                        <li>{fileError.filename}: {fileError.error}</li>
                                                    {/each}
                                                </ul>
                                            </details>
                                        </div>
                                    {/if}
                                    
                                    <!-- Show sub-part errors if any -->
                                    {#if formErrors.subParts.length > 0}
                                        <div class="mt-2">
                                            <details class="text-sm">
                                                <summary class="cursor-pointer font-medium">
                                                    {formErrors.subParts.length} object part issue{formErrors.subParts.length === 1 ? '' : 's'}
                                                </summary>
                                                <ul class="mt-2 pl-5 list-disc space-y-1 text-xs">
                                                    {#each formErrors.subParts as partError}
                                                        <li>{partError.name}: {partError.error}</li>
                                                    {/each}
                                                </ul>
                                            </details>
                                        </div>
                                    {/if}
                                    
                                    {#if formErrors.imageUpload}
                                        <div class="mt-1 text-sm text-red-700 dark:text-red-400">
                                            {formErrors.imageUpload}
                                        </div>
                                    {/if}
                                    
                                    <div class="mt-2 text-xs text-red-600 dark:text-red-400">
                                        If the problem persists, try refreshing the page or checking your internet connection.
                                    </div>
                                </div>
                            </div>
                        </div>
                    {/if}

                    <!-- Title -->
                    <div class="mb-4">
                        <label for="title" class="block text-sm font-medium mb-1.5 text-neutral-700 dark:text-neutral-300 flex items-center">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M12 20h9"></path>
                                <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
                            </svg>
                            Title*
                        </label>
                        <Textarea 
                            id="title" 
                            class="w-full p-3 border rounded-md text-sm bg-white dark:bg-neutral-950 border-neutral-200 dark:border-neutral-700 focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500" 
                            bind:value={title} 
                            placeholder="This is what people will see on their homepage so try to make it interesting" 
                        />
                        {#if errors.title}
                            <p class="text-red-500 text-sm mt-1">{errors.title}</p>
                        {/if}
                    </div>

                    <!-- Description -->
                    <div class="mb-5">
                        <label for="description" class="block text-sm font-medium mb-1.5 text-neutral-700 dark:text-neutral-300 flex items-center">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                <polyline points="14 2 14 8 20 8"></polyline>
                                <line x1="16" y1="13" x2="8" y2="13"></line>
                                <line x1="16" y1="17" x2="8" y2="17"></line>
                                <polyline points="10 9 9 9 8 9"></polyline>
                            </svg>
                            Description*
                        </label>
                        <Textarea 
                            id="description" 
                            class="w-full p-3 border rounded-md text-sm bg-white dark:bg-neutral-950 border-neutral-200 dark:border-neutral-700 focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500 min-h-[120px]" 
                            bind:value={description} 
                            placeholder="You can add any additional context about your object or how you came into possession of it."
                        />
                        {#if errors.description}
                            <p class="text-red-500 text-sm mt-1">{errors.description}</p>
                        {/if}
                    </div>
                     
                    <!-- Attributes and Tags wrapper for side-by-side on lg screens -->
                    <div class="flex flex-col lg:flex-row lg:gap-6 mb-6">
                        <!-- Object Attributes Component -->
                        <div class="w-full lg:w-1/2 mb-6 lg:mb-0">
                            <div class="mb-2 text-sm font-medium text-neutral-700 dark:text-neutral-300 flex items-center">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"></path>
                                    <line x1="7" y1="7" x2="7.01" y2="7"></line>
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
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"></path>
                                    <line x1="7" y1="7" x2="7.01" y2="7"></line>
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
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <rect x="3" y="3" width="7" height="7"></rect>
                                <rect x="14" y="3" width="7" height="7"></rect>
                                <rect x="14" y="14" width="7" height="7"></rect>
                                <rect x="3" y="14" width="7" height="7"></rect>
                            </svg>
                            Object Parts (Optional)
                        </h3>
                        <p class="text-sm text-neutral-500 dark:text-neutral-400 mb-3">You can add parts to your mystery object if it consists of multiple components.</p>

                        <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-sm p-3">
                            <MysteryObjectSubParts
                                mysteryObjectId={null}
                                bind:subParts={mysteryObjectSubParts}
                                on:update={handleSubPartsUpdate}
                            />
                        </div>
                    </div>

                    <!-- Media upload component -->
                    <div class="mb-6">
                        <h3 class="block text-sm font-medium mb-2 text-neutral-700 dark:text-neutral-300 flex items-center">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                                <circle cx="8.5" cy="8.5" r="1.5"></circle>
                                <polyline points="21 15 16 10 5 21"></polyline>
                            </svg>
                            Media Files*
                        </h3>
                        <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-sm p-3">
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

                    <!-- Action Buttons -->
                    <div class="flex justify-start gap-3 border-t border-neutral-100 dark:border-neutral-800 pt-4 mt-6">
                        <Button
                            on:click={handlePost}
                            variant="default" 
                            class="text-sm py-1 px-4 bg-teal-600 hover:bg-teal-700 text-white rounded-full"
                            disabled={isSubmitting}
                        >
                            {#if isSubmitting}
                                <div class="animate-spin mr-2 h-4 w-4 border-2 border-white border-t-transparent rounded-full"></div>
                                Creating Post...
                            {:else}
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M20 6L9 17l-5-5"></path>
                                </svg>
                                Post Your Stuff
                            {/if}
                        </Button>
                        <Button
                            variant="outline"
                            on:click={() => goto('/')}
                            class="text-sm py-1 px-3 border-neutral-300 dark:border-neutral-700 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-full"
                            disabled={isSubmitting}
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <line x1="19" y1="12" x2="5" y2="12"></line>
                                <polyline points="12 19 5 12 12 5"></polyline>
                            </svg>
                            Cancel
                        </Button>
                    </div>
                </Card.Content>
            </Card.Root>
        </form>
    </div>
</div>
