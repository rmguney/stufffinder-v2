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
        auth: ''
    };
    
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
                throw new Error('Failed to fetch post details');
            }
            
            const postData = await response.json();
            originalPost = postData;
            
            // Check if current user is the author
            if (postData.author !== $activeUser) {
                errors.auth = "You don't have permission to edit this post";
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
            errors.auth = "Error loading post data";
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

    // Handle post update form submission
    async function handleUpdate() {
        // Reset error state
        errors = {
            title: '',
            media: '',
            description: '',
            auth: ''
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
                throw new Error(`Failed to update post: ${updateResponse.status} - ${errorText}`);
            }

            const responseData = await updateResponse.json();
            console.log("Post updated successfully:", responseData);

            // Update main image if needed
            if (mediaFiles.length > 0 && mediaFiles[0].file) {
                const imageFormData = new FormData();
                imageFormData.append('file', mediaFiles[0].file);

                await fetch(`${PUBLIC_API_URL}/api/posts/${postId}/mysteryObjects/${mysteryObjectId}/set-image`, {
                    method: 'POST',
                    headers: getAuthHeader(),
                    body: imageFormData
                });
            }

            // Handle media files upload (for new files only)
            for (let i = 0; i < mediaFiles.length; i++) {
                const mediaItem = mediaFiles[i];
                
                // Skip files that don't have a file property (existing files)
                if (!mediaItem.file) continue;
                
                const mediaFormData = new FormData();
                mediaFormData.append('file', mediaItem.file);
                mediaFormData.append('type', mediaItem.type || 'image');

                await fetch(`${PUBLIC_API_URL}/api/mysteryObjects/${mysteryObjectId}/upload-media`, {
                    method: 'POST',
                    headers: getAuthHeader(),
                    body: mediaFormData
                });
            }

            // Handle sub-parts updates
            if (mysteryObjectSubParts.length > 0) {
                // For each sub-part: update existing ones, create new ones
                for (let i = 0; i < mysteryObjectSubParts.length; i++) {
                    const subPart = mysteryObjectSubParts[i];
                    const cleanSubPart = prepareSubPartForApi(subPart);
                    
                    if (subPart.id) {
                        // Update existing sub-part
                        await fetch(`${PUBLIC_API_URL}/api/mysteryObjects/${mysteryObjectId}/subParts/${subPart.id}`, {
                            method: 'PUT',
                            headers: {
                                'Content-Type': 'application/json',
                                ...getAuthHeader()
                            },
                            body: JSON.stringify(cleanSubPart)
                        });
                    } else {
                        // Create new sub-part
                        await fetch(`${PUBLIC_API_URL}/api/mysteryObjects/${mysteryObjectId}/subParts`, {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json',
                                ...getAuthHeader()
                            },
                            body: JSON.stringify(cleanSubPart)
                        });
                    }
                }
                
                // Find deleted sub-parts by comparing with original sub-parts
                if (originalPost?.mysteryObject?.subParts) {
                    const originalSubPartIds = new Set(originalPost.mysteryObject.subParts.map(p => p.id));
                    const currentSubPartIds = new Set(mysteryObjectSubParts.filter(p => p.id).map(p => p.id));
                    
                    // Find IDs that were in the original but not in current -> these need to be deleted
                    for (const id of originalSubPartIds) {
                        if (!currentSubPartIds.has(id)) {
                            // Delete this sub-part
                            await fetch(`${PUBLIC_API_URL}/api/mysteryObjects/${mysteryObjectId}/subParts/${id}`, {
                                method: 'DELETE',
                                headers: getAuthHeader()
                            });
                        }
                    }
                }
            }

            // Navigate back to the thread page
            goto(`/thread/${postId}`);
        } catch (error) {
            console.error('Error updating post:', error);
            errors.auth = `Failed to update post: ${error.message}`;
        } finally {
            saveLoading = false;
        }
    }
</script>

<div class="flex flex-col items-center bg-change dark:bg-dark shifting p-3 sm:p-4 md:p-5">
    <div class="w-full max-w-7xl mx-auto">
        <form class="w-full" on:submit|preventDefault={handleUpdate}>
            <Card.Root class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden">
                <Card.Header class="p-4 border-b border-neutral-100 dark:border-neutral-800">
                    <Card.Title class="text-xl font-semibold text-neutral-900 dark:text-white">
                        Edit Mystery Object
                    </Card.Title>
                    <Card.Description class="text-sm text-neutral-600 dark:text-neutral-400">
                        Update the details about your mystery object
                    </Card.Description>
                </Card.Header>

                {#if isLoading}
                    <div class="flex justify-center items-center py-16">
                        <div class="inline-block h-10 w-10 border-4 border-neutral-200 dark:border-neutral-800 border-t-teal-600 dark:border-t-teal-500 rounded-full animate-spin"></div>
                    </div>
                {:else}
                    <Card.Content class="p-4 sm:p-6">
                        {#if errors.auth}
                            <div class="bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300 p-3 rounded-md mb-4 border border-red-200 dark:border-red-800/50">
                                {errors.auth}
                            </div>
                        {/if}

                        <!-- Title -->
                        <div class="mb-4">
                            <label for="title" class="block text-sm font-medium mb-1.5 text-neutral-700 dark:text-neutral-300">Title*</label>
                            <Textarea 
                                id="title" 
                                class="w-full p-3 border rounded-md text-sm bg-white dark:bg-neutral-950 border-neutral-200 dark:border-neutral-700 focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500" 
                                bind:value={title} 
                                placeholder="Post title" 
                            />
                            {#if errors.title}
                                <p class="text-red-500 text-sm mt-1">{errors.title}</p>
                            {/if}
                        </div>

                        <!-- Description -->
                        <div class="mb-4">
                            <label for="description" class="block text-sm font-medium mb-1.5 text-neutral-700 dark:text-neutral-300">Description*</label>
                            <Textarea 
                                id="description" 
                                class="w-full p-3 border rounded-md text-sm bg-white dark:bg-neutral-950 border-neutral-200 dark:border-neutral-700 focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500 min-h-[120px]" 
                                bind:value={description} 
                                placeholder="Post description and any additional context"
                            />
                            {#if errors.description}
                                <p class="text-red-500 text-sm mt-1">{errors.description}</p>
                            {/if}
                        </div>
                        
                        <!-- Tags Section -->
                        <div class="mb-6">
                            <label class="block text-sm font-medium mb-1.5 text-neutral-700 dark:text-neutral-300 flex items-center">
                                <span>Tags</span>
                                <span class="ml-2 px-2 py-0.5 bg-neutral-100 dark:bg-neutral-800 text-xs rounded-full text-neutral-700 dark:text-neutral-300">
                                    {tags.length} added
                                </span>
                            </label>
                            <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-sm p-3">
                                <Query bind:tags={tags} bind:labels={labels} />
                            </div>
                        </div>

                        <!-- Media upload component with consistent styling -->
                        <div class="mb-6">
                            <label class="block text-sm font-medium mb-1.5 text-neutral-700 dark:text-neutral-300">Media Files*</label>
                            <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-sm p-3">
                                <MediaUploader
                                    bind:mediaFiles={mediaFiles}
                                    bind:errors={errors}
                                    on:update={handleMediaUpdate}
                                />
                                {#if errors.media}
                                    <p class="text-red-500 text-sm mt-1">{errors.media}</p>
                                {/if}
                            </div>
                        </div>

                        <!-- Separator -->
                        <div class="h-px bg-neutral-200 dark:bg-neutral-800 my-6"></div>

                        <!-- Mystery Object Section -->
                        <h3 class="text-lg font-medium mb-4 text-neutral-950 dark:text-white">Mystery Object Properties</h3>
                        
                        <!-- Object Attributes Component -->
                        <div class="mb-6">
                            <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-sm p-3">
                                <ObjectAttributes 
                                    bind:attributeValues={attributeValues}
                                    bind:activeAttributes={activeAttributes}
                                    on:update={handleAttributesUpdate}
                                    on:valuechange={handleAttributeValueChange}
                                />
                            </div>
                        </div>

                        <!-- Sub-parts component -->
                        <div class="mb-6">
                            <h3 class="text-base font-medium mb-3 text-neutral-800 dark:text-neutral-300 border-b border-neutral-200 dark:border-neutral-700 pb-2">Object Parts</h3>
                            <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-sm p-3">
                                <p class="text-sm text-neutral-600 dark:text-neutral-400 mb-4">
                                    You can add, edit, or remove parts of your mystery object.
                                </p>
                                <MysteryObjectSubParts
                                    mysteryObjectId={mysteryObjectId}
                                    bind:subParts={mysteryObjectSubParts}
                                    on:update={handleSubPartsUpdate}
                                />
                            </div>
                        </div>
                    </Card.Content>

                    <!-- Action Buttons -->
                    <div class="px-4 py-3 border-t border-neutral-100 dark:border-neutral-800 flex flex-wrap gap-3 justify-end bg-neutral-50 dark:bg-neutral-900">
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
                        <Button 
                            on:click={handleUpdate}
                            variant="default" 
                            class="text-sm py-1 px-3 bg-teal-600 hover:bg-teal-700 text-white rounded-full"
                            disabled={saveLoading}
                        >
                            {#if saveLoading}
                                <span class="inline-block h-3.5 w-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin mr-1.5"></span>
                                Saving Changes...
                            {:else}
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
                                    <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                                </svg>
                                Save Changes
                            {/if}
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
    
    form {
        animation: fadeIn 0.4s ease-out;
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
