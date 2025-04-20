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

    let handlePost = async () => {
        // Reset error state
        errors = {
            title: '',
            media: '',
            description: ''
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
            return; // Don't proceed if validation fails
        }

        try {
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
            console.log("Creating post with JSON data");
            const createResponse = await fetch(`${PUBLIC_API_URL}/api/posts/create-json`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(simplifiedPost),
                credentials: 'include'
            });

            if (!createResponse.ok) {
                let errorMessage = `Failed to create post: ${createResponse.status} ${createResponse.statusText}`;
                try {
                    const errorData = await createResponse.json();
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    }
                } catch (parseError) {
                    console.error("Error parsing response:", parseError);
                }
                throw new Error(errorMessage);
            }

            const responseData = await createResponse.json();
            console.log("Post created successfully:", responseData);

            // Then upload image separately if available
            if (mediaFiles.length > 0) {
                const imageFormData = new FormData();
                imageFormData.append('file', mediaFiles[0].file);

                console.log("Uploading main image");
                try {
                    await fetch(`${PUBLIC_API_URL}/api/posts/${responseData.postId}/mysteryObjects/${responseData.mysteryObjectId}/set-image`, {
                        method: 'POST',
                        body: imageFormData,
                        credentials: 'include'
                    });
                } catch (imageError) {
                    console.error("Error uploading main image:", imageError);
                }
            }

            console.log("Post created successfully:", responseData);
            
            // Upload media files
            if (mediaFiles.length > 0) {
                console.log(`Uploading ${mediaFiles.length} media files`);

                for (let i = 0; i < mediaFiles.length; i++) {
                    const mediaItem = mediaFiles[i];
                    const mediaFormData = new FormData();
                    mediaFormData.append('file', mediaItem.file);
                    mediaFormData.append('type', mediaItem.type || 'image');

                    try {
                        console.log(`Uploading media file ${i+1}/${mediaFiles.length}`);
                        const mediaResponse = await fetch(`${PUBLIC_API_URL}/api/mysteryObjects/${responseData.mysteryObjectId}/upload-media`, {
                            method: 'POST',
                            body: mediaFormData,
                            credentials: 'include'
                        });

                        if (!mediaResponse.ok) {
                            console.error(`Failed to upload media file ${i+1}:`, mediaResponse.status, mediaResponse.statusText);
                        } else {
                            console.log(`Media file ${i+1} uploaded successfully`);
                        }
                    } catch (mediaError) {
                        console.error(`Error uploading media file ${i+1}:`, mediaError);
                    }
                }
            }

            // Add sub-parts to the mystery object
            if (mysteryObjectSubParts.length > 0) {
                console.log(`Adding ${mysteryObjectSubParts.length} sub-parts to mystery object`);

                // Process sub-parts sequentially to avoid race conditions
                for (let i = 0; i < mysteryObjectSubParts.length; i++) {
                    try {
                        console.log(`Adding sub-part ${i+1}/${mysteryObjectSubParts.length}`);

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
                            console.error(`Failed to add sub-part ${i+1}:`, subPartResponse.status, subPartResponse.statusText);
                            const errorText = await subPartResponse.text();
                            console.error('Error response:', errorText);
                        } else {
                            console.log(`Sub-part ${i+1} added successfully`);
                        }
                    } catch (subPartError) {
                        console.error(`Error adding sub-part ${i+1}:`, subPartError);
                    }
                }
            }

            // Update thread store and navigate to home
            threadStore.update(prev => [...prev, responseData]);
            goto('/');
        } catch (error) {
            console.error('Error creating post:', error);
            alert(`Failed to create post: ${error.message}`);
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
                    <!-- Title -->
                    <div class="mb-4">
                        <label for="title" class="block text-sm font-medium mb-1.5 text-neutral-700 dark:text-neutral-300 flex items-center">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                                <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd" />
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
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                                <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 2a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7zm0 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd" />
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
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                                <path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd" />
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
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                        </svg>
                        Post Your Stuff
                    </Button>
                        <Button
                            variant="outline"
                            on:click={() => goto('/')}
                            class="text-sm py-1 px-3 border-neutral-300 dark:border-neutral-700 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-full"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm.707-10.293a1 1 0 00-1.414-1.414l-3 3a1 1 0 000 1.414l3 3a1 1 0 001.414-1.414L9.414 11H13a1 1 0 100-2H9.414l1.293-1.293z" clip-rule="evenodd" />
                            </svg>
                            Cancel
                        </Button>
                    </div>
                </Card.Content>
            </Card.Root>
        </form>
    </div>
</div>
